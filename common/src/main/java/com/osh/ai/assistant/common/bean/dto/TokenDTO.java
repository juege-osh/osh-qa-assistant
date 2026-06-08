package com.osh.ai.assistant.common.bean.dto;

import lombok.Data;

/**
 * 定义了返回给前端的token是由哪些信息生成的
 */
@Data
public class TokenDTO {

    /**
     * 用户id
//     */
    private Long id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 头像存放路径,格式如:resources/type/20230523/123.jpg
     */
    private String avatarPath;

    /**
     * 账号角色: ADMIN / USER
     */
    private String role;
}
