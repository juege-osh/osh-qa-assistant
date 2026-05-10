package com.osh.ai.assistant.consumer.bean.vo;

import com.osh.ai.assistant.common.enums.ChatMessageTypeEnum;
import com.osh.ai.assistant.common.util.EnumUtil;
import lombok.Data;

import java.util.Date;

/**
  * 聊天消息视图对象
  */
@Data
public class ChatMessageVO {
    /**
     * 主键
     */
    private Long id;
    /**
     * chat表的主键
     */
    private Long chatId;
    /**
     * 消息类型
     * @see ChatMessageTypeEnum
     */
    private Integer type;
    /**
     * 消息内容
     */
    private String message;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * {@link #type}描述
     */
    private String typeDesc;

    public String getTypeDesc() {
        return EnumUtil.getDescByCode(type, ChatMessageTypeEnum.class);
    }
}
