package com.osh.ai.assistant.common.util;

import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.ex.BizEx;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OssUtilTest {

    @Test
    void normalizeObjectKeyShouldUseCustomPathInsteadOfPrependingBasePath() {
        // 防止回归成 basePath + customPath，导致 resources/resources 这类重复目录。
        OssUtil ossUtil = buildOssUtil("resources/");

        String objectKeyPrefix = ReflectionTestUtils.invokeMethod(ossUtil, "normalizeObjectKey", "resources/documents");

        assertThat(objectKeyPrefix).isEqualTo("resources/documents/");
    }

    @Test
    void normalizeObjectKeyShouldFallbackToBasePathWhenCustomPathIsBlank() {
        // 未传业务路径时才使用 basePath，对齐原 osh-backend OssUtil 语义。
        OssUtil ossUtil = buildOssUtil("/common/image");

        String objectKeyPrefix = ReflectionTestUtils.invokeMethod(ossUtil, "normalizeObjectKey", "");

        assertThat(objectKeyPrefix).isEqualTo("common/image/");
    }

    @Test
    void shouldExposeMissingRequiredOssConfigsClearly() {
        UploadProperties uploadProperties = new UploadProperties();
        uploadProperties.setStorageType("oss");

        OssUtil ossUtil = new OssUtil();
        ReflectionTestUtils.setField(ossUtil, "uploadProperties", uploadProperties);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(ossUtil, "requireOssProperties"))
                .isInstanceOf(BizEx.class)
                .hasMessageContaining("OSS存储已启用，但缺少必要配置")
                .hasMessageContaining("upload.oss.access-key")
                .hasMessageContaining("upload.oss.secret-key")
                .hasMessageContaining("upload.oss.endpoint")
                .hasMessageContaining("upload.oss.bucket-name");
    }

    private OssUtil buildOssUtil(String basePath) {
        UploadProperties uploadProperties = new UploadProperties();
        uploadProperties.getOss().setAccessKey("test-access-key");
        uploadProperties.getOss().setSecretKey("test-secret-key");
        uploadProperties.getOss().setEndpoint("https://example.com");
        uploadProperties.getOss().setBucketName("test-bucket");
        uploadProperties.getOss().setBasePath(basePath);

        OssUtil ossUtil = new OssUtil();
        ReflectionTestUtils.setField(ossUtil, "uploadProperties", uploadProperties);
        return ossUtil;
    }
}
