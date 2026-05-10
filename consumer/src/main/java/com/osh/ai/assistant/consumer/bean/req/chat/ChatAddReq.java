package com.osh.ai.assistant.consumer.bean.req.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会话新增入参类

 */
@Data
public class ChatAddReq {
    /**
     * 所属应用
     */
    @NotNull
    private Long appId;
    /**
     * 会话名称
     */
    @NotBlank
    private String chatName;
}
