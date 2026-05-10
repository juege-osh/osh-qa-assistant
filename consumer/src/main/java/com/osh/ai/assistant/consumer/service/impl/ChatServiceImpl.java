package com.osh.ai.assistant.consumer.service.impl;

import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.bean.entity.ChatDO;
import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.consumer.bean.dto.ChatDTO;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatReq;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatAddReq;
import com.osh.ai.assistant.consumer.bean.req.chat.ChatRenameReq;
import com.osh.ai.assistant.consumer.bean.vo.ChatVO;
import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;
import com.osh.ai.assistant.consumer.manager.InvokeManager;
import com.osh.ai.assistant.consumer.mapper.ChatMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.osh.ai.assistant.consumer.service.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 聊天会话业务实现类
 *

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl extends ServiceImpl<ChatMapper, ChatDO> implements ChatService {
    @Resource
    private ExecutorService executorService;
    @Resource
    private ChatMessageService chatMessageService;
    @Resource
    private UserService userService;
    @Resource
    private AppService appService;
    @Resource
    private InvokeManager invokeManager;
    @Resource
    private AiChatService aiChatService;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public List<ChatVO> listRecent(Long appId) {
        LambdaQueryWrapper<ChatDO> lq = Wrappers.<ChatDO>lambdaQuery()
            .eq(ChatDO::getAppId, appId)
            .orderByDesc(ChatDO::getModifiedTime)
            .last(" limit 100 ");
        return ConvertUtil.convert(list(lq), ChatVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SseEmitter connect(Long chatId) {
        // 0L 表示永不超时
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(chatId, emitter);
        // 连接完成或出错时清理
        emitter.onCompletion(() -> emitters.remove(chatId));
        emitter.onTimeout(() -> emitters.remove(chatId));
        emitter.onError(e -> emitters.remove(chatId));
        return emitter;
    }

    @Override
    public void chat(ChatReq chatReq) {
        SseEmitter sseEmitter = emitters.get(chatReq.getChatId());
        if (sseEmitter == null) {
            throw new BizEx("无效会话");
        }
        AppDO app = obtainApp(chatReq.getChatId());
        ChatDTO chatDTO = ConvertUtil.convert(chatReq, ChatDTO.class);
        chatDTO.setConversationId(String.valueOf(chatReq.getChatId()));
        UserDO crtUser = userService.getById(UserContext.getUserId());
        chatDTO.setAppKey(crtUser.getAppKey());
        InvokeRecordBuilder builder = invokeManager.initInvokeRecordBuild(chatDTO,crtUser,app);
        // 保存用户输入的聊天信息
        chatMessageService.addUserMessage(chatReq);
        executorService.execute(() -> {
            String assistantMessage = aiChatService.doChat(sseEmitter,app,builder);
            // 保存ai响应的完整结果
            chatMessageService.addAssistantMessage(chatReq.getChatId(),assistantMessage);
        });
    }

    /**
     * 获取应用信息
     */
    private AppDO obtainApp(Long chatId) {
        ChatDO chatDO = getById(chatId);
        if (chatDO == null) {
            throw new BizEx("会话不存在");
        }
        AppDO appDO = appService.getById(chatDO.getAppId());
        if (appDO == null) {
            throw new BizEx("应用信息不存在");
        }
        return appDO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatVO add(ChatAddReq addReq) {
        ChatDO chatDO = new ChatDO();
        chatDO.setAppId(addReq.getAppId());
        chatDO.setChatName(addReq.getChatName());
        save(chatDO);
        return ConvertUtil.convert(chatDO, ChatVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(ChatRenameReq req) {
        ChatDO chatDO = getById(req.getId());
        if (chatDO == null) {
            throw new BizEx("会话不存在");
        }
        LambdaUpdateWrapper<ChatDO> luw = new LambdaUpdateWrapper<>();
        luw.eq(ChatDO::getId, req.getId())
            .set(ChatDO::getChatName, req.getChatName());
        update(new ChatDO(), luw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        removeById(id);
        chatMessageService.deleteByChatId(id);
    }
}
