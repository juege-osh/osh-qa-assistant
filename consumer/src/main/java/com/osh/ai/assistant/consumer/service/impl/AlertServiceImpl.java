package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.osh.ai.assistant.consumer.config.properties.AlertProperties;
import com.osh.ai.assistant.consumer.service.AlertService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AlertServiceImpl implements AlertService {

    @Resource
    private AlertProperties alertProperties;

    private final JavaMailSender mailSender;

    private final Map<String, Instant> lastNotifyAtMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> activeAlertMap = new ConcurrentHashMap<>();

    public AlertServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    @Override
    public void notifyOnce(String key, String title, String body) {
        if (!canSendAlert()) {
            return;
        }
        Instant now = Instant.now();
        Instant lastNotifyAt = lastNotifyAtMap.get(key);
        long dedupeSeconds = Math.max(1, alertProperties.getDedupeMinutes()) * 60;
        if (lastNotifyAt != null && now.isBefore(lastNotifyAt.plusSeconds(dedupeSeconds))) {
            log.info("skip duplicated alert, key={}, title={}", key, title);
            activeAlertMap.put(key, Boolean.TRUE);
            return;
        }
        sendMail(title, body);
        lastNotifyAtMap.put(key, now);
        activeAlertMap.put(key, Boolean.TRUE);
    }

    @Override
    public void notifyResolved(String key, String title, String body) {
        if (!canSendAlert() || !alertProperties.isRecoveryEnabled()) {
            return;
        }
        if (!Boolean.TRUE.equals(activeAlertMap.get(key))) {
            return;
        }
        sendMail(title, body);
        activeAlertMap.remove(key);
        lastNotifyAtMap.remove(key);
    }

    private boolean canSendAlert() {
        if (!alertProperties.isEnabled()) {
            return false;
        }
        if (mailSender == null) {
            log.warn("alert service enabled but JavaMailSender is unavailable");
            return false;
        }
        if (CollUtil.isEmpty(alertProperties.getRecipients())) {
            log.warn("alert service enabled but recipients are empty");
            return false;
        }
        return true;
    }

    private void sendMail(String title, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(alertProperties.getRecipients().toArray(String[]::new));
            message.setSubject(buildSubject(title));
            message.setText(StrUtil.blankToDefault(body, "无告警详情"));
            mailSender.send(message);
            log.info("alert mail sent, title={}, recipients={}", title, alertProperties.getRecipients().size());
        } catch (Exception e) {
            log.error("send alert mail failed, title={}", title, e);
        }
    }

    private String buildSubject(String title) {
        String prefix = StrUtil.blankToDefault(alertProperties.getSubjectPrefix(), "[OSH QA Assistant]");
        return prefix + " " + title;
    }
}
