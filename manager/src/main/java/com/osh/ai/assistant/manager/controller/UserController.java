package com.osh.ai.assistant.manager.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.manager.bean.req.user.UserPageReq;
import com.osh.ai.assistant.manager.bean.vo.UserVO;
import com.osh.ai.assistant.manager.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/queryPage")
    public Result<List<UserVO>> queryPage(@RequestBody UserPageReq pageReq) {
        return userService.queryPage(pageReq);
    }

    @GetMapping("/queryById")
    public Result<UserVO> queryById(@RequestParam("id") Long id) {
        return Result.buildSuccess(userService.queryById(id));
    }
}
