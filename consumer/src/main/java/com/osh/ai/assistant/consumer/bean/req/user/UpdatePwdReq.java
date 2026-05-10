package com.osh.ai.assistant.consumer.bean.req.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改密码请求参数
 */
@Data
public class UpdatePwdReq {
    /**
     * 主键
     */
    @NotNull
    private Long id;
    /**
     * 原始密码
     */
    @NotBlank
    private String originalPwd;
    /**
     * 新密码
     */
    @NotBlank
    private String newPwd;
}
