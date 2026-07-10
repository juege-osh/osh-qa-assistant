package com.osh.ai.assistant.common.service;

import com.osh.ai.assistant.common.enums.UploadPathEnum;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * OSS 对象存储服务
 */
public interface OssService {

    /**
     * 上传 MultipartFile 到指定业务目录。
     *
     * @param file 上传文件
     * @param pathEnum 业务目录类型，为空时使用 RESOURCE
     * @param bizId 可选业务 ID，会参与对象 key 分层
     * @return OSS 对象 key
     */
    String upload(MultipartFile file, UploadPathEnum pathEnum, String bizId);

    /**
     * 上传已读取的文件内容，供 StorageService 避免重复读取 MultipartFile。
     *
     * @return OSS 对象 key
     */
    String upload(String originalFilename, String contentType, byte[] bytes, UploadPathEnum pathEnum, String bizId);

    /**
     * 生成临时签名 URL，用于私有对象或需要限时访问的场景。
     */
    String getLimitedUrl(String fileKey, int minute);

    /**
     * 判断对象 key 是否存在。
     */
    boolean existsFileKey(String fileKey);

    /**
     * 删除对象 key 对应的远端文件。
     */
    boolean deleteFile(String fileKey);

    /**
     * 将 OSS 对象下载到指定的本地文件。
     * 用于文档解析、向量化等需要本地读取文件内容的场景。
     */
    void downloadToFile(String fileKey, File destFile);
}
