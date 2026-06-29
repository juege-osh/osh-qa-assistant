package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.RagAcceptanceBatchSaveReq;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.RagAcceptanceRunDefaultBatchReq;
import com.osh.ai.assistant.consumer.bean.vo.RagAcceptanceBatchVO;
import com.osh.ai.assistant.consumer.service.RagAcceptanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ragAcceptance")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RagAcceptanceController {
    private final RagAcceptanceService ragAcceptanceService;

    @PostMapping("/saveBatch")
    public Result<Long> saveBatch(@RequestBody @Valid RagAcceptanceBatchSaveReq req) {
        return Result.buildSuccess(ragAcceptanceService.saveBatch(req));
    }

    @PostMapping("/runDefaultBatch")
    public Result<Long> runDefaultBatch(@RequestBody @Valid RagAcceptanceRunDefaultBatchReq req) {
        return Result.buildSuccess(ragAcceptanceService.runDefaultBatch(req));
    }

    @GetMapping("/listMine")
    public Result<List<RagAcceptanceBatchVO>> listMine() {
        return Result.buildSuccess(ragAcceptanceService.listMine());
    }

    @GetMapping("/detail")
    public Result<RagAcceptanceBatchVO> detail(@RequestParam("id") Long id) {
        return Result.buildSuccess(ragAcceptanceService.detail(id));
    }
}
