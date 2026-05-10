package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatReq;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatAddReq;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatRenameReq;
import com.osh.ai.assistant.consumer.bean.vo.ChatVO;
import com.osh.ai.assistant.consumer.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
  * 聊天会话Controller层
  */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ChatController {

    private final ChatService chatService;

    /**
     * 保存会话
     */
    @PostMapping("/add")
    public Result<ChatVO> add(@RequestBody @Validated ChatAddReq addReq) {
        return Result.buildSuccess(chatService.add(addReq));
    }

    /**
     * 重命名会话
     */
    @PostMapping("/rename")
    public Result<Void> rename(@RequestBody @Validated ChatRenameReq req) {
        chatService.rename(req);
        return Result.buildSuccessMsg("修改成功");
    }

    /**
     * 列出当前用户最近的聊天会话
     */
    @GetMapping("/listRecent")
    public Result<List<ChatVO>> listRecent(@RequestParam("appId") Long appId) {
        return Result.buildSuccess(chatService.listRecent(appId));
    }

    /**
     * SSE 连接端点
     */
    @GetMapping("/connect")
    public SseEmitter connect(@RequestParam("chatId") Long chatId) {
        return chatService.connect(chatId);
    }

    /**
     * 聊天
     */
    @PostMapping(value = "/chat")
    public Result<Void> chat(@RequestBody @Validated ChatReq chatReq) {
        chatService.chat(chatReq);
        return Result.buildSuccessMsg("处理完成");
    }

    /**
     * 按id删除
     */
    @GetMapping("/deleteById")
    public Result<Void> deleteById(@RequestParam("id") Long id) {
        chatService.deleteById(id);
        return Result.buildSuccessMsg("删除成功");
    }
}
