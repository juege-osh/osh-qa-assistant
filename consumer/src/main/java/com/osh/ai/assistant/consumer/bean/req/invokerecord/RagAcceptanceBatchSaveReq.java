package com.osh.ai.assistant.consumer.bean.req.invokerecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RagAcceptanceBatchSaveReq {
    private Long id;
    @NotBlank(message = "验收批次名称不能为空")
    private String batchName;
    private String appName;
    private String sceneType;
    private String knowledgeScope;
    private String releaseVersion;
    private String experimentVersion;
    private String versionRemark;
    private String quickView;
    private String quickViewDesc;
    private String testerName;
    private String testDate;
    private String summaryConclusion;
    private String nextAction;
    @NotEmpty(message = "至少要选择一条验收记录")
    private List<RagAcceptanceItemSaveReq> items;
}
