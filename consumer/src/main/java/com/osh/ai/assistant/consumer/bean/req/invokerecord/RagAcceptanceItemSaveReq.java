package com.osh.ai.assistant.consumer.bean.req.invokerecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RagAcceptanceItemSaveReq {
    private Long id;
    @NotNull(message = "调用记录id不能为空")
    private Long invokeRecordId;
    private Long invokeRecordDetailId;
    @NotBlank(message = "测试编号不能为空")
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
}
