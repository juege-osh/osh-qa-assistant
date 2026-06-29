package com.osh.ai.assistant.common.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("rag_acceptance_item")
public class RagAcceptanceItemDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long batchId;
    private Long invokeRecordId;
    private Long invokeRecordDetailId;
    private String testCaseNo;
    private String questionType;
    private String userQuestion;
    private String expectedKnowledge;
    private String actualAnswerSummary;
    private String actualAnswer;
    private String failReason;
    private String hitConclusion;
    private String groundedConclusion;
    private String readableConclusion;
    private String gracefulFailureConclusion;
    private String hitRateConclusion;
    private String completenessConclusion;
    private String followUpCategory;
    private String followUpAction;
    private String remark;
    private String invokeStatus;
    private String modelName;
    private String appName;
    private String libName;
    private Integer costTime;
    private Long costToken;
    private Date createdTime;
    private Date modifiedTime;
}
