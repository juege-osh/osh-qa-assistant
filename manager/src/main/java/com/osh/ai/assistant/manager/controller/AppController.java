package com.osh.ai.assistant.manager.controller;
import com.osh.ai.assistant.common.bean.res.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.osh.ai.assistant.manager.bean.req.app.AppPageReq;
import com.osh.ai.assistant.manager.bean.vo.AppVO;
import com.osh.ai.assistant.manager.service.AppService;
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
     * 应用信息分页查询
     */
    @PostMapping("/queryPage")
    public Result<List<AppVO>> queryPage(@RequestBody AppPageReq pageReq) {
        return  appService.queryPage(pageReq);
    }
}
