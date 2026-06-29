package com.osh.ai.assistant.consumer.bean.req.invokerecord;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagAcceptanceRunQuestionReq {
    @NotBlank(message = "测试编号不能为空")
    private String testCaseNo;
    private String questionType;
    @NotBlank(message = "用户问题不能为空")
    private String userQuestion;
    private String expectedKnowledge;
}
