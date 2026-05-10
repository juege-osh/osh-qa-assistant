package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.InvokeRecordPageReq;
import com.osh.ai.assistant.consumer.bean.vo.InvokeRecordOverviewVO;
import com.osh.ai.assistant.consumer.bean.vo.InvokeRecordVO;
import com.osh.ai.assistant.consumer.service.InvokeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
  * 调用记录Controller层
  */
@RestController
@RequestMapping("/invokeRecord")
@RequiredArgsConstructor
@Slf4j
@Validated
public class InvokeRecordController {

    private final InvokeRecordService invokeRecordService;
    @PostMapping("/queryPage")
    public Result<List<InvokeRecordVO>> queryPage(@RequestBody InvokeRecordPageReq pageReq) {
        return  invokeRecordService.queryPage(pageReq);
    }

    @PostMapping("/queryOverview")
    public Result<InvokeRecordOverviewVO> queryOverview() {
        return invokeRecordService.queryOverview();
    }
}
