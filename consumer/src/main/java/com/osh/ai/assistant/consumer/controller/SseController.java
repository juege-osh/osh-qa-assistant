package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.consumer.service.SseService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Server-Sent Events (SSE) 是一种允许服务器向客户端推送实时更新的技术,特点:
 * 1.单向通信:服务器可以主动向客户端发送数据，而客户端无需频繁轮询服务器请求数据。
 * 2.基于HTTP协议进行长连接:使用标准的 text/event-stream MIME 类型来传输数据
 * 3.数据格式:每条消息以 data: 开头，以两个换行符 \n\n 结尾
 * 4.事件类型：可以为每条消息指定事件类型
 */
@RestController
@RequestMapping("/sse")
public class SseController {
    @Resource
    private SseService sseService;

    /**
     * 基于sse完成页面的流式输出
     * GET http://localhost:9000/consumer/sse/readFile
     */
    @GetMapping(value = "/readFile"
            // 返回{@link SseEmitter}时response的content-type默认就是text/event-stream,可省略
            ,produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter readFile() {
        // 抛出异常了也能被全局异常处理拦截
//        System.out.println(1/0);
        SseEmitter emitter = new SseEmitter();
        // 这里不能使用同步的方法调用,如果是同步方法调用,由于主线程被同步阻塞，return emitter
        // 就是在所有数据处理完成后才能被执行了,这就导致前端在连接建立后一次性收到所有缓存的数据
//        sseService.readFile(emitter);
        new Thread(() -> sseService.readFile(emitter)).start();
        return emitter;
    }
}
