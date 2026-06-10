package com.osh.ai.assistant.common.config.properties;

import cn.hutool.core.util.StrUtil;
import com.osh.ai.assistant.common.util.PathUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;



@ConfigurationProperties(prefix = "upload")
@Data
public class UploadProperties {
    /**
     * 存储文件的绝对路径,格式如: e:/a/
     */
    private String staticDir;
    /**
     * 存储方式: local 或 oss
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
    }

    public boolean isOssStorage() {
        return StrUtil.equalsIgnoreCase(storageType, "oss");
    }

    @Data
    public static class Oss {
        /**
         * S3 兼容服务 accessKey
         */
        private String accessKey;
        /**
         * S3 兼容服务 secretKey
         */
        private String secretKey;
        /**
         * S3 兼容服务 endpoint
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
         * 对象 key 统一前缀
         */
        private String basePath = "";
        /**
         * 是否使用 path-style 访问，Cloudflare R2 等 S3 兼容服务通常需要开启
         */
        private Boolean pathStyleAccess = true;
    }
}
