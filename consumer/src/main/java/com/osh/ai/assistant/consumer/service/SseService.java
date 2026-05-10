package com.osh.ai.assistant.consumer.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


@Service
public class SseService {
    public void readFile(SseEmitter emitter) {
        ClassPathResource classPathResource = new ClassPathResource("app.txt");
        try (InputStream is = classPathResource.getInputStream();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 构造事件,最终发送的数据格式为:data: line\n\n
                SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event().data(line);
                // 发送构造好的事件到连接的客户端
                emitter.send(sseEventBuilder);
                Thread.sleep(500);
            }
            // 完成事件流并关闭连接
            emitter.complete();
        } catch (Exception e) {
            // 发生错误时完成事件流并关闭连接
            emitter.completeWithError(e);
        }
    }
}
