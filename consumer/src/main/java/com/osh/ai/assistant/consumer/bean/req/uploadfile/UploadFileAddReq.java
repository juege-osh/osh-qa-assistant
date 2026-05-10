package com.osh.ai.assistant.consumer.bean.req.uploadfile;
import lombok.Data;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
/**
 * 文件信息新增入参类

 */
@Data
public class UploadFileAddReq {
    /**
     * knowledge_lib表的主键
     */
    @NotNull
    private Long libId;
    /**
     * 文件存放路径,格式如:resources/type/20230523/123.jpg
     */
    @NotBlank
    private String storePath;
    /**
     * 原始文件名称
     */
    @NotBlank
    private String originalFileName;
}
