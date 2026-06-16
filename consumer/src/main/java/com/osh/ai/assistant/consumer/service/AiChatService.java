package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * ai聊天处理
 */
public interface AiChatService {
    /**
     * ai聊天
     * @param sseEmitter
     * @param app 当前应用信息
     * @param builder 调用主记录
     * @param closeOnComplete true表示本轮回答结束后关闭SSE，false表示保留长连接
     * @return ai最终的完整结果
     */
    String doChat(SseEmitter sseEmitter, AppDO app, InvokeRecordBuilder builder, boolean closeOnComplete);
}
