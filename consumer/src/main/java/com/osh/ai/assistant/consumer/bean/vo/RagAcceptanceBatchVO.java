package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class RagAcceptanceBatchVO {
    private Long id;
    private Long appId;
    private Long libId;
    private String batchName;
    private String appName;
    private String sceneType;
    private String knowledgeScope;
    private String releaseVersion;
    private String experimentVersion;
    private Long activeExperimentId;
    private String activeExperimentName;
    private String activeSplitStrategy;
    private String versionRemark;
    private String quickView;
    private String quickViewDesc;
    private String testerName;
    private Date testDate;
    private String summaryConclusion;
    private String nextAction;
    private Date createdTime;
    private Date modifiedTime;
    private Integer itemCount;
    private Integer passCount;
    private Integer followUpCount;
    private List<RagAcceptanceItemVO> items;
}
