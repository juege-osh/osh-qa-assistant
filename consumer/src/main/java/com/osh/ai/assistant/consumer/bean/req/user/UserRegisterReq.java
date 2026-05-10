package com.osh.ai.assistant.consumer.bean.req.user;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;import java.util.Date;
/**
 * 用户表新增入参类
 */
@Data
public class UserRegisterReq {
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
