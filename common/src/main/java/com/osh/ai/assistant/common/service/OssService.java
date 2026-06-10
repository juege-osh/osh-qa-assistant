package com.osh.ai.assistant.common.service;

import com.osh.ai.assistant.common.enums.UploadPathEnum;
import org.springframework.web.multipart.MultipartFile;

/**
 * OSS 对象存储服务
 */
public interface OssService {

    String upload(MultipartFile file, UploadPathEnum pathEnum, String bizId);

    String upload(String originalFilename, String contentType, byte[] bytes, UploadPathEnum pathEnum, String bizId);

    String getLimitedUrl(String fileKey, int minute);

    boolean existsFileKey(String fileKey);

    boolean deleteFile(String fileKey);
}
