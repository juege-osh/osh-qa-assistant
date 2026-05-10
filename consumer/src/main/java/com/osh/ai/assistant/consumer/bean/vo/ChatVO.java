package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

import java.util.Date;

/**
  * 聊天会话视图对象
  */
@Data
public class ChatVO {
    /**
     * 主键
     */
    private Long id;
    /**
     * app表的id
     */
    private Long appId;
    /**
     * 聊天名称
     */
    private String chatName;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 修改时间
     */
    private Date modifiedTime;
}
