package com.osh.ai.assistant.backend.controller;

import com.osh.ai.assistant.backend.bean.req.AuthLoginReq;
import com.osh.ai.assistant.backend.service.AuthService;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Resource
    private AuthService authService;

    @GetMapping("/getCode")
    public Result<CodeVO> getCode() {
        return Result.buildSuccess(authService.getCode());
    }

    @PostMapping("/login")
    public Result<LoginResultVO> login(@RequestBody @Valid AuthLoginReq loginReq) {
        return Result.buildSuccess(authService.login(loginReq));
    }
}
