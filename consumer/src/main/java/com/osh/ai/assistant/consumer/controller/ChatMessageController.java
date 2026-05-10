package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.vo.ChatMessageVO;
import com.osh.ai.assistant.consumer.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
  * 聊天会话Controller层
  */
@RestController
@RequestMapping("/chatMessage")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    /**
     * 查询指定聊天的聊天记录
     */
    @GetMapping("/listHistory")
    public Result<List<ChatMessageVO>> listHistory(@RequestParam("chatId") Long chatId) {
        return Result.buildSuccess(chatMessageService.listHistory(chatId));
    }
}
