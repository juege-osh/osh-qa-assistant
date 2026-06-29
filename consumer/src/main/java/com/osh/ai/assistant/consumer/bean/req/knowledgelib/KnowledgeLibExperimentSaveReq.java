package com.osh.ai.assistant.consumer.bean.req.knowledgelib;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeLibExperimentSaveReq {
    @NotNull(message = "知识库不能为空")
    private Long libId;
    private String versionName;
    @NotBlank(message = "调试问题不能为空")
    private String queryText;
    @NotBlank(message = "切分策略不能为空")
    private String splitStrategy;
    @NotNull(message = "原始召回数不能为空")
    private Integer rawHitCount;
    @NotNull(message = "重排后召回数不能为空")
    private Integer rerankHitCount;
    @NotBlank(message = "诊断结论不能为空")
    private String diagnosisTitle;
    @NotBlank(message = "归类编码不能为空")
    private String categoryCode;
    @NotBlank(message = "归类名称不能为空")
    private String categoryLabel;
    private String rawTopSummary;
    private String rerankTopSummary;
    private Integer recommended;
}
