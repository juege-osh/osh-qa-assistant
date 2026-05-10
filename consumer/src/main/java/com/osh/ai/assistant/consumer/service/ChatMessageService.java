package com.osh.ai.assistant.consumer.service;


import com.osh.ai.assistant.common.bean.entity.ChatMessageDO;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatReq;
import com.osh.ai.assistant.consumer.bean.vo.ChatMessageVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
  * 聊天消息业务类
  */
public interface ChatMessageService extends IService<ChatMessageDO> {
    List<ChatMessageVO> listHistory(Long chatId);

    void addUserMessage(ChatReq chatReq);

    void addAssistantMessage(Long chatId, String message);

    void deleteByChatId(Long chatId);
}
