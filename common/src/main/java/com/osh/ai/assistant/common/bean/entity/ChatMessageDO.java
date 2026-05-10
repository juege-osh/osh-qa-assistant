package com.osh.ai.assistant.common.bean.entity;

import com.osh.ai.assistant.common.enums.ChatMessageTypeEnum;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 聊天消息

 */
@Data
@TableName("chat_message")
public class ChatMessageDO {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
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
    @TableField(fill = FieldFill.INSERT)
    private Date createdTime;
}
