package com.osh.ai.assistant.consumer.bean.req.knowledgelib;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeLibExperimentRecommendReq {
    @NotNull(message = "知识库不能为空")
    private Long libId;
    @NotNull(message = "实验版本不能为空")
    private Long id;
}
