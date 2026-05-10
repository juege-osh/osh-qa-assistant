package com.osh.ai.assistant.consumer.bean.req.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class ApiChatReq {
    /**
     * 系统颁发的appKey,可在用户中心查看
     */
    @NotBlank
    private String appKey;
    /**
     * 用户输入
     */

    @NotBlank
    private String userInput;

    /**
     * 应用id
     */
    @NotNull
    private Long appId;
    /**
     * 唯一会话标识,可自己生成,用于区分不同会话
     */
    @NotBlank
    private String chatId;
}
