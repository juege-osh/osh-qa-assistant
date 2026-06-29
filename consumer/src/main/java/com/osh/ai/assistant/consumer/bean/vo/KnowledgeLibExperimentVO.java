package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

import java.util.Date;

@Data
public class KnowledgeLibExperimentVO {
    private Long id;
    private Long libId;
    private String versionName;
    private String queryText;
    private String splitStrategy;
    private Integer rawHitCount;
    private Integer rerankHitCount;
    private String diagnosisTitle;
    private String categoryCode;
    private String categoryLabel;
    private String rawTopSummary;
    private String rerankTopSummary;
    private Integer recommended;
    private Date createdTime;
    private Date modifiedTime;
}
