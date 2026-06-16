package com.osh.ai.assistant.common.util;

import cn.hutool.core.util.IdUtil;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.ex.BizEx;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;

/**
 * S3 兼容 OSS 工具类，当前用于对接 Cloudflare R2。
 * <p>
 * AmazonS3Client 是线程安全的，内部维护了 HTTP 连接池。
 * 这里采用懒加载单例，避免每次操作都新建客户端导致连接资源浪费。
 */
@Slf4j
@Component
public class OssUtil {

    @Resource
    private UploadProperties uploadProperties;

    /** 懒加载单例，volatile + double-checked locking 保证线程安全 */
    private volatile AmazonS3Client s3Client;

    /**
     * 获取共享的 S3 客户端。首次调用时创建，后续复用同一实例。
     * AmazonS3Client 本身线程安全，可安全地在多线程中共享。
     */
    public AmazonS3Client getS3Client() {
        AmazonS3Client client = this.s3Client;
        if (client == null) {
            synchronized (this) {
                client = this.s3Client;
                if (client == null) {
                    client = buildS3Client();
                    this.s3Client = client;
                }
            }
        }
        return client;
    }

    @PreDestroy
    public void shutdown() {
        AmazonS3Client client = this.s3Client;
        if (client != null) {
            try {
                client.shutdown();
            } catch (Exception e) {
                log.warn("关闭 S3 客户端时出错", e);
            }
        }
    }

    private AmazonS3Client buildS3Client() {
        UploadProperties.Oss oss = requireOssProperties();
        BasicAWSCredentials credentials = new BasicAWSCredentials(oss.getAccessKey(), oss.getSecretKey());
        AmazonS3Client client = new AmazonS3Client(credentials);
        client.setEndpoint(oss.getEndpoint());
        if (Boolean.TRUE.equals(oss.getPathStyleAccess())) {
            // Cloudflare R2 等 S3 兼容服务通常要求 path-style，否则 bucket 会被拼进域名。
            client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());
        }
        return client;
    }

    /**
     * 上传文件内容并返回对象 key。这里沿用原项目 uuid_原文件名格式，方便人工排查来源文件。
     */
    public String uploadFile(String originalFilename, String contentType, byte[] bytes, String customPath) {
        UploadProperties.Oss oss = requireOssProperties();
        String fileKey = normalizeObjectKey(customPath) + IdUtil.fastSimpleUUID() + CommonConstants.UNDER_LINE + originalFilename;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        if (StringUtils.isNotBlank(contentType)) {
            metadata.setContentType(contentType);
        }
        PutObjectRequest request = new PutObjectRequest(
                oss.getBucketName(),
                fileKey,
                new ByteArrayInputStream(bytes),
                metadata
        );
        getS3Client().putObject(request);
        return fileKey;
    }

    public String getFullFilePath(String fileKey) {
        UploadProperties.Oss oss = requireOssProperties();
        String publicDomain = StringUtils.removeEnd(oss.getPublicDomain(), CommonConstants.SLASH);
        return publicDomain + CommonConstants.SLASH + StringUtils.removeStart(fileKey, CommonConstants.SLASH);
    }

    /**
     * 对已编码的对象 key 先解码再签名，避免中文或空格文件名签名失败。
     */
    public String getSignedUrl(String fileKey, int expireMinutes) {
        String decodedKey = URLDecoder.decode(fileKey, StandardCharsets.UTF_8);
        Date expiration = new Date(System.currentTimeMillis() + (long) expireMinutes * 60 * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                requireOssProperties().getBucketName(),
                decodedKey
        ).withExpiration(expiration);
        return getS3Client().generatePresignedUrl(request).toString();
    }

    public boolean deleteFile(String fileKey) {
        try {
            getS3Client().deleteObject(requireOssProperties().getBucketName(), normalizeFileKey(fileKey));
            return true;
        } catch (Exception e) {
            log.error("删除 OSS 对象失败, fileKey={}", fileKey, e);
            return false;
        }
    }

    public boolean existsFileKey(String fileKey) {
        if (StringUtils.isBlank(fileKey)) {
            return false;
        }
        return getS3Client().doesObjectExist(requireOssProperties().getBucketName(), normalizeFileKey(fileKey));
    }

    /**
     * 将 OSS 对象下载到指定的本地文件。
     * 用于 OSS 模式下，业务代码需要读取文件内容（如文档解析、向量化）时，
     * 先下载到临时文件再交给下游处理。
     *
     * @param fileKey OSS 对象 key
     * @param destFile 目标本地文件
     */
    public void downloadToFile(String fileKey, File destFile) {
        try {
            S3Object s3Object = getS3Client().getObject(
                    new GetObjectRequest(requireOssProperties().getBucketName(), normalizeFileKey(fileKey))
            );
            try (InputStream inputStream = s3Object.getObjectContent()) {
                Files.copy(inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new BizEx("从 OSS 下载文件失败: " + fileKey);
        }
    }

    private UploadProperties.Oss requireOssProperties() {
        UploadProperties.Oss oss = uploadProperties.getOss();
        String missingConfigMessage = uploadProperties.buildMissingOssConfigMessage();
        if (StringUtils.isNotBlank(missingConfigMessage)) {
            throw new BizEx(missingConfigMessage);
        }
        return oss;
    }

    private String normalizeObjectKey(String customPath) {
        UploadProperties.Oss oss = requireOssProperties();
        // 对齐原 osh-backend 行为：有 customPath 时直接使用它；未传时才回退 basePath。
        String keyPrefix = StringUtils.isNotBlank(customPath) ? customPath : oss.getBasePath();
        return normalizePath(keyPrefix);
    }

    private String normalizePath(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        String normalizedPath = path.replace("\\", CommonConstants.SLASH);
        normalizedPath = StringUtils.removeStart(normalizedPath, CommonConstants.SLASH);
        return normalizedPath.endsWith(CommonConstants.SLASH) ? normalizedPath : normalizedPath + CommonConstants.SLASH;
    }

    private String normalizeFileKey(String fileKey) {
        return StringUtils.removeStart(fileKey, CommonConstants.SLASH);
    }
}
