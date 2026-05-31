package com.osh.ai.assistant.consumer.service;


import com.osh.ai.assistant.common.bean.entity.ChatDO;
import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatReq;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatAddReq;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatRenameReq;
import com.osh.ai.assistant.consumer.bean.vo.ChatVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
  * 聊天会话业务类
  */
public interface ChatService extends IService<ChatDO> {
    /**
     *
     * @param appId 所属应用id
     */
    List<ChatVO> listRecent(Long appId);
    SseEmitter connect(Long chatId);

    void chat(ChatReq chatReq);

    ChatVO add(ChatAddReq addReq);

    void rename(ChatRenameReq req);

    void deleteById(Long id);

    AppDO requireOwnedApp(Long appId);

    ChatDO requireOwnedChat(Long chatId);
}
