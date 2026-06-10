package com.osh.ai.assistant.common.util;

import cn.hutool.core.util.IdUtil;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.ex.BizEx;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * S3 兼容 OSS 工具类
 */
@Component
public class OssUtil {

    @Resource
    private UploadProperties uploadProperties;

    public AmazonS3Client createS3Client() {
        UploadProperties.Oss oss = requireOssProperties();
        BasicAWSCredentials credentials = new BasicAWSCredentials(oss.getAccessKey(), oss.getSecretKey());
        AmazonS3Client s3Client = new AmazonS3Client(credentials);
        s3Client.setEndpoint(oss.getEndpoint());
        if (Boolean.TRUE.equals(oss.getPathStyleAccess())) {
            s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());
        }
        return s3Client;
    }

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
        createS3Client().putObject(request);
        return fileKey;
    }

    public String getFullFilePath(String fileKey) {
        UploadProperties.Oss oss = requireOssProperties();
        String publicDomain = StringUtils.removeEnd(oss.getPublicDomain(), CommonConstants.SLASH);
        return publicDomain + CommonConstants.SLASH + StringUtils.removeStart(fileKey, CommonConstants.SLASH);
    }

    public String getSignedUrl(String fileKey, int expireMinutes) {
        String decodedKey = URLDecoder.decode(fileKey, StandardCharsets.UTF_8);
        Date expiration = new Date(System.currentTimeMillis() + (long) expireMinutes * 60 * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                requireOssProperties().getBucketName(),
                decodedKey
        ).withExpiration(expiration);
        return createS3Client().generatePresignedUrl(request).toString();
    }

    public boolean deleteFile(String fileKey) {
        createS3Client().deleteObject(requireOssProperties().getBucketName(), normalizeFileKey(fileKey));
        return true;
    }

    public boolean existsFileKey(String fileKey) {
        if (StringUtils.isBlank(fileKey)) {
            return false;
        }
        return createS3Client().doesObjectExist(requireOssProperties().getBucketName(), normalizeFileKey(fileKey));
    }

    private UploadProperties.Oss requireOssProperties() {
        UploadProperties.Oss oss = uploadProperties.getOss();
        if (oss == null
                || StringUtils.isAnyBlank(oss.getAccessKey(), oss.getSecretKey(), oss.getEndpoint(), oss.getBucketName())) {
            throw new BizEx("OSS配置不完整");
        }
        return oss;
    }

    private String normalizeObjectKey(String customPath) {
        UploadProperties.Oss oss = requireOssProperties();
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
