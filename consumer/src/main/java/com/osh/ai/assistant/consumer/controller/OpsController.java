package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.req.ops.AlertSelfCheckReq;
import com.osh.ai.assistant.consumer.bean.vo.AlertReadinessVO;
import com.osh.ai.assistant.consumer.bean.vo.AlertSelfCheckVO;
import com.osh.ai.assistant.consumer.service.OpsSelfCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OpsController {
    private final OpsSelfCheckService opsSelfCheckService;

    @GetMapping("/alertReadiness")
    public Result<AlertReadinessVO> alertReadiness() {
        return Result.buildSuccess(opsSelfCheckService.queryAlertReadiness());
    }

    @PostMapping("/alertSelfCheck")
    public Result<AlertSelfCheckVO> alertSelfCheck(@RequestBody @Valid AlertSelfCheckReq req) {
        return Result.buildSuccess(opsSelfCheckService.triggerAlertSelfCheck(req));
    }
}
