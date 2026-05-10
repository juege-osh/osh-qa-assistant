package com.osh.ai.assistant.consumer.bean.req.knowledgelib;
import lombok.Data;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
/**
 * 知识库新增入参类

 */
@Data
public class KnowledgeLibAddReq {
    /**
     * 知识库名称
     */
    @NotBlank
    private String libName;
    /**
     * 知识库描述
     */
    @NotBlank
    private String libDesc;
    /**
     * 图标存放路径,格式如:resources/type/20230523/123.jpg
     */
    private String iconPath;
}
