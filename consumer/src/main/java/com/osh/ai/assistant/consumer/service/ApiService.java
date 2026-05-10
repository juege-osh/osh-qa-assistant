package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.consumer.bean.req.api.ApiChatReq;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
  * 对外api业务类
  */
public interface ApiService {

    SseEmitter chat(ApiChatReq chatReq);
}
