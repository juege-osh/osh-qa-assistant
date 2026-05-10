package com.osh.ai.assistant.consumer.controller;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.req.app.BindLibReq;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.osh.ai.assistant.consumer.bean.req.app.AppPageReq;
import com.osh.ai.assistant.consumer.bean.req.app.AppAddReq;
import com.osh.ai.assistant.consumer.bean.req.app.AppUpdateReq;
import com.osh.ai.assistant.consumer.bean.vo.AppVO;
import com.osh.ai.assistant.consumer.service.AppService;
/**
  * 应用信息Controller层
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AppController {

    private final AppService appService;

    /**
     * 应用信息新增
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Validated AppAddReq addReq) {
        appService.add(addReq);
        return Result.buildSuccessMsg("添加成功");
    }

    /**
     * 应用信息分页查询
     */
    @PostMapping("/queryPage")
    public Result<List<AppVO>> queryPage(@RequestBody AppPageReq pageReq) {
        return  appService.queryPage(pageReq);
    }

    /**
     * 按id删除应用信息
     */
    @GetMapping("/deleteById")
    public Result<Void> deleteById(@RequestParam("id") Long id) {
        appService.deleteById(id);
        return Result.buildSuccessMsg("删除成功");
    }

    /**
     * 按主键id查询应用信息
     */
    @GetMapping("/queryById")
    public Result<AppVO> queryById(@RequestParam("id") Long id) {
        return  Result.buildSuccess(appService.queryById(id));
    }

    /**
     * 按主键id修改应用信息
     */
    @PostMapping("/modifyById")
    public Result<Void> modifyById(@RequestBody @Validated AppUpdateReq updateReq) {
        appService.modifyById(updateReq);
        return Result.buildSuccessMsg("更新成功");
    }

    /**
     * 检查聊天条件
     */
    @GetMapping("/checkChatCondition")
    public Result<Void> checkChatCondition(@RequestParam("id") Long id) {
        appService.checkChatCondition(id);
        return Result.buildSuccessMsg("检测通过");
    }

    /**
     * 解绑知识库
     */
    @GetMapping("/unBindLib")
    public Result<Void> unBindLib(@RequestParam("id") Long id) {
        appService.unBindLib(id);
        return Result.buildSuccessMsg("解绑成功");
    }

    /**
     * 绑定知识库
     */
    @PostMapping("/bindLib")
    public Result<Void> bindLib(@RequestBody @Validated BindLibReq req) {
        appService.bindLib(req);
        return Result.buildSuccessMsg("绑定成功");
    }
}
