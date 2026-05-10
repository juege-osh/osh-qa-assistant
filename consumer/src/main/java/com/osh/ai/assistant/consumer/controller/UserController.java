package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;
import com.osh.ai.assistant.consumer.bean.req.user.LoginReq;
import com.osh.ai.assistant.consumer.bean.req.user.UpdatePwdReq;
import com.osh.ai.assistant.consumer.bean.req.user.UserRegisterReq;
import com.osh.ai.assistant.consumer.bean.req.user.UserUpdateReq;
import com.osh.ai.assistant.consumer.bean.vo.UserVO;
import com.osh.ai.assistant.consumer.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

/**
 * 用户表Controller层
 *
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;
    /**
     * 生成验证码
     */
    @GetMapping("/getCode")
    public Result<CodeVO> getCode() {
        return Result.buildSuccess(userService.getCode());
    }

    @PostMapping("/login")
    public Result<LoginResultVO> login(@RequestBody @Validated LoginReq loginReq) {
        return Result.buildSuccess(userService.login(loginReq));
    }

    @PostMapping("/register")
    public Result<Void> add(@RequestBody @Validated UserRegisterReq registerReq) {
        userService.register(registerReq);
        return Result.buildSuccessMsg("注册成功");
    }

    @PostMapping("/modifyById")
    public Result<Void> modifyById(@RequestBody @Validated UserUpdateReq updateReq) {
        userService.modifyById(updateReq);
        return Result.buildSuccessMsg("修改成功");
    }

    @PostMapping("/updatePwd")
    public Result<Void> updatePwd(@RequestBody @Validated UpdatePwdReq updatePwdReq) {
        userService.updatePwd(updatePwdReq);
        return Result.buildSuccessMsg("密码修改成功");
    }

    /**
     * 通过主键查询一个实体
     */
    @GetMapping("/queryById")
    public Result<UserVO> queryById(@RequestParam("id") Long id) {
        return Result.buildSuccess(userService.queryById(id));
    }
}
