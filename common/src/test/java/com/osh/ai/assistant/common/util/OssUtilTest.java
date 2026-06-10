package com.osh.ai.assistant.common.util;

import com.osh.ai.assistant.common.config.properties.UploadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OssUtilTest {

    @Test
    void normalizeObjectKeyShouldUseCustomPathInsteadOfPrependingBasePath() {
        OssUtil ossUtil = buildOssUtil("resources/");

        String objectKeyPrefix = ReflectionTestUtils.invokeMethod(ossUtil, "normalizeObjectKey", "resources/documents");

        assertThat(objectKeyPrefix).isEqualTo("resources/documents/");
    }

    @Test
    void normalizeObjectKeyShouldFallbackToBasePathWhenCustomPathIsBlank() {
        OssUtil ossUtil = buildOssUtil("/common/image");

        String objectKeyPrefix = ReflectionTestUtils.invokeMethod(ossUtil, "normalizeObjectKey", "");

        assertThat(objectKeyPrefix).isEqualTo("common/image/");
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
