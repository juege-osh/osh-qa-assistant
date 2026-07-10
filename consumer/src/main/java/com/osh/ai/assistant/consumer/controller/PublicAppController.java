package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.req.publicapp.PublicAppChatReq;
import com.osh.ai.assistant.consumer.bean.req.publicapp.PublicAppVerifyPasswordReq;
import com.osh.ai.assistant.consumer.bean.vo.PublicAppAccessVO;
import com.osh.ai.assistant.consumer.bean.vo.PublicAppDetailVO;
import com.osh.ai.assistant.consumer.service.PublicAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 公开应用访问控制器
 */
@RestController
@RequestMapping("/public/app")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicAppController {

    private final PublicAppService publicAppService;

    /**
     * 查询公开应用详情
     */
    @GetMapping("/detail")
    public Result<PublicAppDetailVO> detail(@RequestParam("slug") String slug) {
        return Result.buildSuccess(publicAppService.queryDetail(slug));
    }

    /**
     * 校验访问密码
     */
    @PostMapping("/verifyPassword")
    public Result<PublicAppAccessVO> verifyPassword(@RequestBody @Validated PublicAppVerifyPasswordReq req) {
        return Result.buildSuccess(publicAppService.verifyPassword(req));
    }

    /**
     * 公开应用聊天
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody @Validated PublicAppChatReq req) {
        return publicAppService.chat(req);
    }
}
