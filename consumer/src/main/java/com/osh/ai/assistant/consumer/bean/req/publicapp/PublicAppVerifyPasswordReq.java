package com.osh.ai.assistant.consumer.bean.req.publicapp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公开应用访问密码校验入参
 */
@Data
public class PublicAppVerifyPasswordReq {
    /**
     * 公开访问标识
     */
    @NotBlank
    private String slug;
    /**
     * 访问密码
     */
    @NotBlank
    private String accessPassword;
}
