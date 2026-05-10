package com.osh.ai.assistant.manager.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;
import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.manager.bean.req.manager.*;
import com.osh.ai.assistant.manager.bean.vo.ManagerVO;
import com.osh.ai.assistant.manager.service.ManagerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/manager")
@Validated
@Slf4j
public class ManagerController {
    @Resource
    private ManagerService managerService;

    /**
     * 生成验证码
     */
    @GetMapping("/getCode")
    public Result<CodeVO> getCode() {
        return Result.buildSuccess(managerService.getCode());
    }

    @PostMapping("/login")
    public Result<LoginResultVO> login(@RequestBody @Validated LoginReq loginReq) {
        return Result.buildSuccess(managerService.login(loginReq));
    }

    @GetMapping("/queryById")
    public Result<ManagerVO> queryById(@RequestParam("id") Long id) {
        return Result.buildSuccess(managerService.queryById(id));
    }

    @PostMapping("/modifyById")
    public Result<Void> modifyById(@RequestBody @Validated ManagerUpdateReq updateReq) {
        managerService.modifyById(updateReq);
        return Result.buildSuccessMsg("修改成功");
    }

    @PostMapping("/updatePwd")
    public Result<Void> updatePwd(@RequestBody @Validated UpdatePwdReq updatePwdReq) {
        managerService.updatePwd(updatePwdReq);
        return Result.buildSuccessMsg("密码修改成功");
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Validated ManagerAddReq addReq) {
        managerService.add(addReq);
        return Result.buildSuccessMsg("新增成功");
    }

    @PostMapping("/queryPage")
    public Result<List<ManagerVO>> queryPage(@RequestBody ManagerPageReq pageReq) {
        return managerService.queryPage(pageReq);
    }

    @GetMapping("/deleteById")
    public Result<Void> deleteById(@RequestParam("id") Long id) {
        managerService.deleteById(id);
        return Result.buildSuccessMsg("删除成功");
    }
}
