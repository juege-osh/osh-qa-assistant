package com.osh.ai.assistant.consumer.service.impl;

import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.consumer.bean.dto.ChatDTO;
import com.osh.ai.assistant.consumer.bean.req.api.ApiChatReq;
import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;
import com.osh.ai.assistant.consumer.manager.InvokeManager;
import com.osh.ai.assistant.consumer.service.AiChatService;
import com.osh.ai.assistant.consumer.service.ApiService;
import com.osh.ai.assistant.consumer.service.AppService;
import com.osh.ai.assistant.consumer.service.UserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;

/**
 * 对外api业务实现类
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {
    @Resource
    private ExecutorService executorService;
    @Resource
    private AppService appService;
    @Resource
    private UserService userService;
    @Resource
    private InvokeManager invokeManager;
    @Resource
    private AiChatService aiChatService;
    @Override
    public SseEmitter chat(ApiChatReq chatReq) {
        SseEmitter emitter = new SseEmitter(0L);
        AppDO app = appService.getById(chatReq.getAppId());
        ChatDTO chatDTO = ConvertUtil.convert(chatReq, ChatDTO.class);
        // 设置会话id
        chatDTO.setConversationId(chatReq.getAppId() + CommonConstants.UNDER_LINE + chatReq.getChatId());
        UserDO crtUser = userService.selectByAppKey(chatReq.getAppKey());
        InvokeRecordBuilder builder = invokeManager.initInvokeRecordBuild(chatDTO,crtUser,app);
        executorService.execute(() -> {
            aiChatService.doChat(emitter,app,builder);
        });
        return emitter;
    }
}
