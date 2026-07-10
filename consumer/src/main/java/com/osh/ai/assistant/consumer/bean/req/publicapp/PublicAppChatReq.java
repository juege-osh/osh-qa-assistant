package com.osh.ai.assistant.consumer.bean.req.publicapp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公开应用聊天入参
 */
@Data
public class PublicAppChatReq {
    /**
     * 公开访问标识
     */
    @NotBlank
    private String slug;
    /**
     * 访客侧会话标识
     */
    @NotBlank
    private String visitorId;
    /**
     * 用户输入
     */
    @NotBlank
    private String userInput;
    /**
     * 密码访问模式下的临时访问凭证
     */
    private String accessToken;
}
