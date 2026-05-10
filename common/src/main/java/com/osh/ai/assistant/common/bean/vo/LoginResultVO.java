package com.osh.ai.assistant.common.bean.vo;

import lombok.Data;

/**
* 文件上传结果
*/
@Data
public class LoginResultVO {
    /**
     * 用户id
     */
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
     * 生成的token
     */
    private String token;
}
