package com.osh.ai.assistant.consumer.service.impl;

import com.osh.ai.assistant.common.bean.entity.ChatMessageDO;
import com.osh.ai.assistant.common.enums.ChatMessageTypeEnum;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatReq;
import com.osh.ai.assistant.consumer.bean.vo.ChatMessageVO;
import com.osh.ai.assistant.consumer.mapper.ChatMessageMapper;
import com.osh.ai.assistant.consumer.service.ChatMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 聊天消息业务实现类

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessageDO> implements ChatMessageService {

    @Override
    public List<ChatMessageVO> listHistory(Long chatId) {
        LambdaQueryWrapper<ChatMessageDO> lq = Wrappers.<ChatMessageDO>lambdaQuery()
            .eq(ChatMessageDO::getChatId, chatId)
            .orderByAsc(ChatMessageDO::getId)
            ;
        return ConvertUtil.convert(list(lq), ChatMessageVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUserMessage(ChatReq chatReq) {
        ChatMessageDO chatMessage = new ChatMessageDO();
        chatMessage.setChatId(chatReq.getChatId());
        chatMessage.setType(ChatMessageTypeEnum.USER.getCode());
        chatMessage.setMessage(chatReq.getUserInput());
        save(chatMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAssistantMessage(Long chatId, String message) {
        ChatMessageDO chatMessage = new ChatMessageDO();
        chatMessage.setChatId(chatId);
        chatMessage.setType(ChatMessageTypeEnum.ASSISTANT.getCode());
        chatMessage.setMessage(message);
        save(chatMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByChatId(Long chatId) {
        LambdaQueryWrapper<ChatMessageDO> lqw = Wrappers.<ChatMessageDO>lambdaQuery()
            .eq(ChatMessageDO::getChatId, chatId);
        remove(lqw);
    }
}
