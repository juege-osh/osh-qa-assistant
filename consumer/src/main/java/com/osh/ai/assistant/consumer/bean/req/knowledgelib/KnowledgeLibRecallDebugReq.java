package com.osh.ai.assistant.consumer.bean.req.knowledgelib;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeLibRecallDebugReq {
    /**
     * 知识库 id
     */
    @NotNull(message = "知识库不能为空")
    private Long libId;
    /**
     * 调试问题
     */
    @NotBlank(message = "调试问题不能为空")
    private String query;
    /**
     * 召回数量
     */
    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 10, message = "topK 不能大于 10")
    private Integer topK = 5;
}
