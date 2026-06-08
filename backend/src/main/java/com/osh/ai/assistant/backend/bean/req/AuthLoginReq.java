package com.osh.ai.assistant.backend.bean.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLoginReq {
    @NotBlank
    private String username;

    @NotBlank
    private String pwd;

    @NotBlank
    private String captchaId;

    @NotBlank
    private String code;

    @NotBlank
    private String role;
}
