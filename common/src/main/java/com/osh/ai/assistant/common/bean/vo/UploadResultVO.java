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
     * 存储方式: local 或 oss
     */
    private String storageType;
    /**
     * OSS 对象 key
     */
    private String objectKey;
    /**
     * 可访问 URL
     */
    private String url;
}
