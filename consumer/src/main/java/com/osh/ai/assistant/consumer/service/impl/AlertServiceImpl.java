package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.osh.ai.assistant.consumer.config.properties.AlertProperties;
import com.osh.ai.assistant.consumer.service.AlertService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AlertServiceImpl implements AlertService {

    @Resource
    private AlertProperties alertProperties;

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String springMailUsername;

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
        boolean sent = sendMail(title, body);
        if (!sent) {
            return;
        }
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
        boolean sent = sendMail(title, body);
        if (!sent) {
            return;
        }
        activeAlertMap.remove(key);
        lastNotifyAtMap.remove(key);
    }

    @Override
    public void sendSelfCheck(String title, String body) {
        if (!canSendAlert()) {
            throw new IllegalStateException("当前告警邮件配置不可用");
        }
        boolean sent = sendMail(title, body);
        if (!sent) {
            throw new IllegalStateException("告警邮件发送失败，请检查 SMTP、发件人和收件人配置");
        }
    }

    @Override
    public boolean canSendAlertNow() {
        return canSendAlert();
    }

    private boolean canSendAlert() {
        if (!alertProperties.isEnabled()) {
            return false;
        }
        if (mailSender == null) {
            log.warn("alert service enabled but JavaMailSender is unavailable");
            return false;
        }
        List<String> recipients = normalizedRecipients();
        if (CollUtil.isEmpty(recipients)) {
            log.warn("alert service enabled but recipients are empty");
            return false;
        }
        return true;
    }

    private boolean sendMail(String title, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String from = StrUtil.blankToDefault(alertProperties.getFrom(), springMailUsername);
            if (StrUtil.isNotBlank(from)) {
                message.setFrom(from);
            }
            message.setTo(normalizedRecipients().toArray(String[]::new));
            message.setSubject(buildSubject(title));
            message.setText(StrUtil.blankToDefault(body, "无告警详情"));
            mailSender.send(message);
            log.info("alert mail sent, title={}, recipients={}", title, normalizedRecipients().size());
            return true;
        } catch (Exception e) {
            log.error("send alert mail failed, title={}", title, e);
            return false;
        }
    }

    private String buildSubject(String title) {
        String prefix = StrUtil.blankToDefault(alertProperties.getSubjectPrefix(), "[OSH QA Assistant]");
        return prefix + " " + title;
    }

    private List<String> normalizedRecipients() {
        return alertProperties.getRecipients().stream()
            .filter(StrUtil::isNotBlank)
            .map(String::trim)
            .toList();
    }
}
