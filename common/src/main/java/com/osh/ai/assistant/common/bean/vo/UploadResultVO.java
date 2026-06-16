package com.osh.ai.assistant.common.bean.vo;

import lombok.Data;

/**
* 文件上传结果
*

*/
@Data
public class UploadResultVO {
    /**
    * 单位:字节,文件大小
    */
    private Long size;
    /**
    * 原始文件名称
    */
    private String originalFilename;
    /**
    * 文件的相对路径,格式:resources/ type/ 20230523/ 123.jpg
    */
    private String relativePath;
    /**
     * 实际存储方式: local 表示本地静态目录，oss 表示 S3 兼容对象存储。
     */
    private String storageType;
    /**
     * OSS 对象 key，仅 OSS 模式返回，用于后续签名访问、删除或存在性检查。
     */
    private String objectKey;
    /**
     * OSS 公开访问 URL。未配置 public-domain 或本地存储模式下为空。
     */
    private String url;
}
