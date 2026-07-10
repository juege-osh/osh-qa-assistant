package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;
import com.osh.ai.assistant.consumer.bean.req.user.LoginReq;
import com.osh.ai.assistant.consumer.bean.req.user.UserRegisterReq;
import com.osh.ai.assistant.consumer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消费端登录前接口。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ConsumerAuthController {

    private final UserService userService;

    @GetMapping("/getCode")
    public Result<CodeVO> getCode() {
        return Result.buildSuccess(userService.getCode());
    }

    @PostMapping("/login")
    public Result<LoginResultVO> login(@RequestBody @Validated LoginReq loginReq) {
        return Result.buildSuccess(userService.login(loginReq));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Validated UserRegisterReq registerReq) {
        userService.register(registerReq);
        return Result.buildSuccessMsg("注册成功");
    }
}
