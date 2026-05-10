package com.osh.ai.assistant.consumer.bean.req.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 个人信息修改请求参数
 */
@Data
public class UserUpdateReq {
    /**
     * 主键
     */
    @NotNull
    private Long id;
    /**
     * 头像存放路径,格式如:resources/type/20230523/123.jpg
     */
    private String avatarPath;
}
