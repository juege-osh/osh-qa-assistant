package com.osh.ai.assistant.consumer.bean.req.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会话重命名请求
 */
@Data
public class ChatRenameReq {
    /**
     * 会话id
     */
    @NotNull(message = "会话id不能为空")
    private Long id;
    /**
     * 会话名称
     */
    @NotBlank(message = "会话名称不能为空")
    private String chatName;
}
