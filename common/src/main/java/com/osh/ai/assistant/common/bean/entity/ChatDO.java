package com.osh.ai.assistant.common.bean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 聊天会话

 */
@Data
@TableName("chat")
public class ChatDO {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * app表的主键
     */
    private Long appId;
    /**
     * 聊天名称
     */
    private String chatName;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createdTime;
    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date modifiedTime;
}
