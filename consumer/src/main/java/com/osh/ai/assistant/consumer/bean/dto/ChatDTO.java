package com.osh.ai.assistant.consumer.bean.dto;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class ChatDTO {
    /**
     * 系统颁发的appKey,可在用户中心查看
     */
    private String appKey;
    /**
     * 请求id,可用于追踪定位请求
     */
    private String requestId;
    /**
     * 用户输入
     */
    private String userInput;
    /**
     * 标识当前对话
     */
    private String conversationId;

    public ChatDTO(){
        this.requestId = IdUtil.fastSimpleUUID();
    }
}
