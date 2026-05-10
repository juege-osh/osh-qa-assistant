package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.consumer.bean.req.api.ApiChatReq;
import com.osh.ai.assistant.consumer.service.ApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ApiController {
    private final ApiService apiService;

    /**
     * 智能问答
     */
    @PostMapping(value = "/chat",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody @Validated ApiChatReq chatReq) {
        return apiService.chat(chatReq);
    }
}
