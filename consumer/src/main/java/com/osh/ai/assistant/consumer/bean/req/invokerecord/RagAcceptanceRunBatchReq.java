package com.osh.ai.assistant.consumer.bean.req.invokerecord;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RagAcceptanceRunBatchReq {
    @NotNull(message = "应用不能为空")
    private Long appId;
    @NotBlank(message = "验收批次名称不能为空")
    private String batchName;
    private String sceneType;
    private String testerName;
    private String summaryConclusion;
    private String nextAction;
    @Valid
    @NotEmpty(message = "至少要提供一条真实问题")
    private List<RagAcceptanceRunQuestionReq> questions;
}
