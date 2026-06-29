package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.osh.ai.assistant.common.bean.dto.TokenDTO;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.consumer.bean.req.ops.AlertSelfCheckReq;
import com.osh.ai.assistant.consumer.bean.vo.AlertReadinessVO;
import com.osh.ai.assistant.consumer.bean.vo.AlertSelfCheckVO;
import com.osh.ai.assistant.consumer.config.properties.AlertProperties;
import com.osh.ai.assistant.consumer.service.AlertService;
import com.osh.ai.assistant.consumer.service.OpsSelfCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpsSelfCheckServiceImpl implements OpsSelfCheckService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlertService alertService;
    private final AlertProperties alertProperties;

    @Value("${spring.mail.host:}")
    private String springMailHost;

    @Value("${spring.mail.port:}")
    private String springMailPort;

    @Value("${spring.mail.username:}")
    private String springMailUsername;

    @Override
    public AlertReadinessVO queryAlertReadiness() {
        List<String> recipients = normalizeList(alertProperties.getRecipients());
        List<String> allowedUsers = normalizeList(alertProperties.getSelfCheckAllowedUsers());
        boolean smtpHostConfigured = StrUtil.isNotBlank(springMailHost);
        boolean smtpPortConfigured = StrUtil.isNotBlank(springMailPort);
        boolean smtpUsernameConfigured = StrUtil.isNotBlank(springMailUsername) || StrUtil.isNotBlank(alertProperties.getFrom());
        boolean recipientsConfigured = CollUtil.isNotEmpty(recipients);
        boolean mailSenderReady = alertService.canSendAlertNow();
        String status;
        String message;
        if (!alertProperties.isEnabled()) {
            status = "DISABLED";
            message = "当前环境未开启最小故障告警。";
        } else if (!alertProperties.isSelfCheckEnabled()) {
            status = "SELF_CHECK_DISABLED";
            message = "告警已开启，但当前环境未开启人工自检入口。";
        } else if (!smtpHostConfigured || !smtpPortConfigured || !recipientsConfigured) {
            status = "CONFIG_MISSING";
            message = "告警链路缺少 SMTP 主机、端口或收件人配置，当前无法完成真实演练。";
        } else if (!mailSenderReady) {
            status = "SENDER_UNAVAILABLE";
            message = "告警链路配置存在，但 JavaMailSender 当前不可用，请检查运行时装配。";
        } else {
            status = "READY";
            message = "告警链路已具备人工演练条件，可以触发真实自检。";
        }
        return AlertReadinessVO.builder()
            .alertEnabled(alertProperties.isEnabled())
            .selfCheckEnabled(alertProperties.isSelfCheckEnabled())
            .mailSenderReady(mailSenderReady)
            .smtpHostConfigured(smtpHostConfigured)
            .smtpUsernameConfigured(smtpUsernameConfigured)
            .recipientsConfigured(recipientsConfigured)
            .subjectPrefix(alertProperties.getSubjectPrefix())
            .from(StrUtil.blankToDefault(alertProperties.getFrom(), springMailUsername))
            .recipients(recipients)
            .allowedUsers(allowedUsers)
            .status(status)
            .message(message)
            .build();
    }

    @Override
    public AlertSelfCheckVO triggerAlertSelfCheck(AlertSelfCheckReq req) {
        TokenDTO currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizEx("用户未登录");
        }
        if (!alertProperties.isSelfCheckEnabled()) {
            throw new BizEx("当前环境未开启告警自检");
        }
        String operator = StrUtil.blankToDefault(currentUser.getUsername(), String.valueOf(currentUser.getId()));
        List<String> allowedUsers = alertProperties.getSelfCheckAllowedUsers();
        List<String> normalizedAllowedUsers = normalizeList(allowedUsers);
        if (CollUtil.isNotEmpty(normalizedAllowedUsers) && !normalizedAllowedUsers.contains(operator)) {
            throw new BizEx("当前用户无权触发告警自检");
        }
        if (!alertService.canSendAlertNow()) {
            throw new BizEx("当前告警邮件配置不可用，请先检查 SMTP 和收件人配置");
        }

        String scene = normalizeScene(req.getScene());
        String title = buildTitle(scene);
        String body = buildBody(scene, operator, req.getNote());
        alertService.sendSelfCheck(title, body);
        log.info("alert self check triggered, scene={}, operator={}", scene, operator);
        return AlertSelfCheckVO.builder()
            .scene(scene)
            .title(title)
            .status("SENT")
            .operator(operator)
            .alertEnabled(alertProperties.isEnabled())
            .mailSenderReady(Boolean.TRUE)
            .subjectPrefix(alertProperties.getSubjectPrefix())
            .from(StrUtil.blankToDefault(alertProperties.getFrom(), springMailUsername))
            .recipients(normalizeList(alertProperties.getRecipients()))
            .message("告警演练邮件已发送，请检查收件箱和部署日志确认链路")
            .build();
    }

    private String normalizeScene(String rawScene) {
        String scene = StrUtil.blankToDefault(rawScene, "generic").trim().toLowerCase(Locale.ROOT);
        return switch (scene) {
            case "database", "db" -> "database";
            case "qdrant", "vector", "vectorstore" -> "qdrant";
            case "generic", "mail", "alert" -> "generic";
            default -> throw new BizEx("不支持的演练类型，仅支持 database / qdrant / generic");
        };
    }

    private String buildTitle(String scene) {
        return switch (scene) {
            case "database" -> "数据库告警链路人工演练";
            case "qdrant" -> "Qdrant 向量库告警链路人工演练";
            default -> "通用告警链路人工演练";
        };
    }

    private String buildBody(String scene, String operator, String note) {
        String now = LocalDateTime.now().format(TIME_FORMATTER);
        StringBuilder body = new StringBuilder()
            .append("这是一次人工触发的告警链路演练，用于确认邮件通知链路是否可用。")
            .append("\n演练时间: ").append(now)
            .append("\n演练类型: ").append(scene)
            .append("\n触发人: ").append(operator);
        if (StrUtil.isNotBlank(note)) {
            body.append("\n演练备注: ").append(note.trim());
        }
        body.append("\n\n说明: 这不是线上真实故障，请仅用于验证 SMTP、收件人和部署日志链路。");
        return body.toString();
    }

    private List<String> normalizeList(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }
        return values.stream()
            .filter(StrUtil::isNotBlank)
            .map(String::trim)
            .collect(Collectors.toList());
    }
}
