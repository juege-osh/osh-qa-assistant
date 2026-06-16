package com.osh.ai.assistant.common.config.properties;

import cn.hutool.core.util.StrUtil;
import com.osh.ai.assistant.common.util.PathUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;


@ConfigurationProperties(prefix = "upload")
@Data
public class UploadProperties {
    /**
     * 存储文件的绝对路径,格式如: e:/a/
     */
    private String staticDir;
    /**
     * 存储方式: local 保持原有本地落盘行为，oss 切换到 S3 兼容对象存储。
     */
    private String storageType = "local";
    /**
     * OSS 配置
     */
    private Oss oss = new Oss();

    @PostConstruct
    public void init() {
        if (StrUtil.isBlank(staticDir)) {
            throw new RuntimeException("UploadProperties.staticDir未配置");
        }
        staticDir = PathUtil.normalize(staticDir);
        PathUtil.checkAndCreateIfNotExist(staticDir);
        validateOssStorageConfig();
    }

    public boolean isOssStorage() {
        return StrUtil.equalsIgnoreCase(storageType, "oss");
    }

    public void validateOssStorageConfig() {
        if (!isOssStorage()) {
            return;
        }
        String message = buildMissingOssConfigMessage();
        if (StrUtil.isNotBlank(message)) {
            throw new RuntimeException(message);
        }
    }

    public List<String> getMissingOssRequiredConfigs() {
        List<String> missingConfigs = new ArrayList<>();
        Oss currentOss = oss;
        if (currentOss == null || StrUtil.isBlank(currentOss.getAccessKey())) {
            missingConfigs.add("upload.oss.access-key");
        }
        if (currentOss == null || StrUtil.isBlank(currentOss.getSecretKey())) {
            missingConfigs.add("upload.oss.secret-key");
        }
        if (currentOss == null || StrUtil.isBlank(currentOss.getEndpoint())) {
            missingConfigs.add("upload.oss.endpoint");
        }
        if (currentOss == null || StrUtil.isBlank(currentOss.getBucketName())) {
            missingConfigs.add("upload.oss.bucket-name");
        }
        return missingConfigs;
    }

    public String buildMissingOssConfigMessage() {
        List<String> missingConfigs = getMissingOssRequiredConfigs();
        if (missingConfigs.isEmpty()) {
            return "";
        }
        return "OSS存储已启用，但缺少必要配置: " + String.join(", ", missingConfigs);
    }

    /**
     * Cloudflare R2 兼容 S3 API。字段名保持通用 OSS 语义，配置层兼容原项目
     * x-file-storage.cloudflare-r2 前缀，便于从 Nacos 迁移。
     */
    @Data
    public static class Oss {
        /**
         * S3 兼容服务 accessKey。生产环境必须通过环境变量或配置中心注入。
         */
        private String accessKey;
        /**
         * S3 兼容服务 secretKey。禁止提交到代码仓库。
         */
        private String secretKey;
        /**
         * S3 兼容服务 endpoint，例如 Cloudflare R2 endpoint。
         */
        private String endpoint;
        /**
         * 存储桶名称
         */
        private String bucketName;
        /**
         * 公开访问域名
         */
        private String publicDomain;
        /**
         * 未显式传入业务路径时使用的对象 key 前缀。
         */
        private String basePath = "";
        /**
         * 是否使用 path-style 访问，Cloudflare R2 等 S3 兼容服务通常需要开启
         */
        private Boolean pathStyleAccess = true;
    }
}
