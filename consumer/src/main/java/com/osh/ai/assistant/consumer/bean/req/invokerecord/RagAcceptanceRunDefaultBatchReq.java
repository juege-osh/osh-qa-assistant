package com.osh.ai.assistant.consumer.bean.req.invokerecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RagAcceptanceRunDefaultBatchReq {
    @NotNull(message = "应用不能为空")
    private Long appId;
    @NotBlank(message = "验收批次名称不能为空")
    private String batchName;
    private String sceneType;
    private String testerName;
    private String summaryConclusion;
    private String nextAction;
}
