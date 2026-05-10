package com.osh.ai.assistant.consumer.manager;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.consumer.bean.dto.ChatDTO;
import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;


@Component
@Slf4j
public class InvokeManager {
    public InvokeRecordBuilder initInvokeRecordBuild(ChatDTO chatDTO, UserDO crtUser, AppDO app) {
        InvokeRecordBuilder builder = InvokeRecordBuilder.builder()
            .setId(IdWorker.getId())
            .setAppId(app.getId())
            .setLibId(app.getLibId())
            .setStartTime(new Date())
            .setChatDto(chatDTO)
            .setEndUser(crtUser);
        return builder;
    }
}
