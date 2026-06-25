package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.consumer.bean.req.publicapp.PublicAppChatReq;
import com.osh.ai.assistant.consumer.bean.req.publicapp.PublicAppVerifyPasswordReq;
import com.osh.ai.assistant.consumer.bean.vo.PublicAppAccessVO;
import com.osh.ai.assistant.consumer.bean.vo.PublicAppDetailVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 公开应用访问业务类
 */
public interface PublicAppService {
    /**
     * 查询公开应用详情
     */
    PublicAppDetailVO queryDetail(String slug);

    /**
     * 校验访问密码并签发临时访问凭证
     */
    PublicAppAccessVO verifyPassword(PublicAppVerifyPasswordReq req);

    /**
     * 公开应用聊天
     */
    SseEmitter chat(PublicAppChatReq req);
}
