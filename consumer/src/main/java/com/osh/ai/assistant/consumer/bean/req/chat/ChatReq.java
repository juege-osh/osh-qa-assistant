package com.osh.ai.assistant.consumer.bean.req.chat;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
public class ChatReq {
    /**
     * 用户输入
     */

    @NotBlank
    private String userInput;
    /**
     * 聊天会话id
     */
    @NotNull
    private Long chatId;

}
