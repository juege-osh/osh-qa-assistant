package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

import java.util.Date;

@Data
public class KnowledgeLibExperimentVO {
    private Long id;
    private Long libId;
    private String versionName;
    private String queryText;
    private Integer topK;
    private String splitStrategy;
    private String splitConfigJson;
    private Integer rawHitCount;
    private Integer rerankHitCount;
    private String diagnosisTitle;
    private String categoryCode;
    private String categoryLabel;
    private String rawTopSummary;
    private String rerankTopSummary;
    private String noteText;
    private String recommendReason;
    private Integer recommended;
    private Date createdTime;
    private Date modifiedTime;
}
