package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

import java.util.Date;
/**
  * 用户表视图对象
  */
@Data
public class UserVO {
    /**
     * 主键
     */
    private Long id;
    /**
     * 用户名
     */
    private String username;

    /**
     * appKey,接口调用时需要传入
     */
    private String appKey;
    /**
     * 注册时间
     */
    private Date registerTime;
    /**
     * 头像存放路径,格式如:resources/type/20230523/123.jpg
     */
    private String avatarPath;
}
