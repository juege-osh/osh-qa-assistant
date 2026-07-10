package com.osh.ai.assistant.common.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("rag_acceptance_batch")
public class RagAcceptanceBatchDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
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
}
