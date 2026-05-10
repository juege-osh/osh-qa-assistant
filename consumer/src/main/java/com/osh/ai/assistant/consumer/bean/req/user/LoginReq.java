package com.osh.ai.assistant.consumer.bean.req.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginReq {
    /**
     * 用户名
     */
    @NotBlank
    private String username;
    /**
     * 密码
     */
    @NotBlank
    private String pwd;
    /**
     * 验证码唯一标识
     */
    @NotBlank
    private String captchaId;
    /**
     * 用户输入的验证码
     */
    @NotBlank
    private String code;
}
