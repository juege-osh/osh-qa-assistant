package com.osh.ai.assistant.common.service.impl;

import com.osh.ai.assistant.common.bean.req.uploadfile.UploadFileReq;
import com.osh.ai.assistant.common.bean.vo.UploadResultVO;
import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.enums.UploadPathEnum;
import com.osh.ai.assistant.common.service.OssService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageServiceImplTest {

    @Test
    void uploadFileShouldKeepLocalStorageByDefault() throws Exception {
        // 默认 local 是兼容性要求，避免引入 OSS 后影响原有本地上传链路。
        Path uploadDir = Files.createTempDirectory("qa-assistant-upload-");
        UploadProperties uploadProperties = new UploadProperties();
        uploadProperties.setStaticDir(uploadDir.toString());
        uploadProperties.init();

        StorageServiceImpl storageService = new StorageServiceImpl();
        ReflectionTestUtils.setField(storageService, "uploadProperties", uploadProperties);
        ReflectionTestUtils.setField(storageService, "ossService", mock(OssService.class));

        UploadFileReq uploadFileReq = new UploadFileReq();
        uploadFileReq.setModule("test");
        uploadFileReq.setFile(new MockMultipartFile("file", "demo.txt", "text/plain", "hello oss".getBytes()));

        UploadResultVO result = storageService.uploadFile(uploadFileReq);

        assertThat(result.getStorageType()).isEqualTo("local");
        assertThat(result.getRelativePath()).startsWith("resources/test/");
        assertThat(result.getObjectKey()).isNull();
        assertThat(Files.exists(uploadDir.resolve(result.getRelativePath()))).isTrue();
    }

    @Test
    void uploadFileShouldSwitchToOssStorageWhenConfigured() throws Exception {
        // OSS 模式不落本地文件，只返回对象 key 和基于 public-domain 拼接的访问 URL。
        Path uploadDir = Files.createTempDirectory("qa-assistant-upload-");
        UploadProperties uploadProperties = new UploadProperties();
        uploadProperties.setStaticDir(uploadDir.toString());
        uploadProperties.setStorageType("oss");
        uploadProperties.getOss().setAccessKey("test-access-key");
        uploadProperties.getOss().setSecretKey("test-secret-key");
        uploadProperties.getOss().setEndpoint("https://example.com");
        uploadProperties.getOss().setBucketName("test-bucket");
        uploadProperties.getOss().setPublicDomain("https://cdn.example.com/");
        uploadProperties.init();

        OssService ossService = mock(OssService.class);
        when(ossService.upload(eq("demo.txt"), eq("text/plain"), any(byte[].class), eq(UploadPathEnum.RESOURCE), eq(null)))
                .thenReturn("resources/20260614/demo.txt");

        StorageServiceImpl storageService = new StorageServiceImpl();
        ReflectionTestUtils.setField(storageService, "uploadProperties", uploadProperties);
        ReflectionTestUtils.setField(storageService, "ossService", ossService);

        UploadFileReq uploadFileReq = new UploadFileReq();
        uploadFileReq.setFile(new MockMultipartFile("file", "demo.txt", "text/plain", "hello oss".getBytes()));

        UploadResultVO result = storageService.uploadFile(uploadFileReq);

        assertThat(result.getStorageType()).isEqualTo("oss");
        assertThat(result.getRelativePath()).isEqualTo("resources/20260614/demo.txt");
        assertThat(result.getObjectKey()).isEqualTo("resources/20260614/demo.txt");
        assertThat(result.getUrl()).isEqualTo("https://cdn.example.com/resources/20260614/demo.txt");
        verify(ossService).upload(eq("demo.txt"), eq("text/plain"), any(byte[].class), eq(UploadPathEnum.RESOURCE), eq(null));
    }

    @Test
    void uploadPropertiesShouldFailFastWhenOssStorageLacksRequiredConfig() throws Exception {
        Path uploadDir = Files.createTempDirectory("qa-assistant-upload-");
        UploadProperties uploadProperties = new UploadProperties();
        uploadProperties.setStaticDir(uploadDir.toString());
        uploadProperties.setStorageType("oss");

        assertThatThrownBy(uploadProperties::init)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OSS存储已启用，但缺少必要配置")
                .hasMessageContaining("upload.oss.access-key")
                .hasMessageContaining("upload.oss.secret-key")
                .hasMessageContaining("upload.oss.endpoint")
                .hasMessageContaining("upload.oss.bucket-name");
    }
}
