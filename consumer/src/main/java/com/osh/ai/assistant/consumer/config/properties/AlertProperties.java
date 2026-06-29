package com.osh.ai.assistant.consumer.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "ai-assistant.alert")
public class AlertProperties {
    /**
     * 是否开启最小故障告警
     */
    private boolean enabled = false;
    /**
     * 邮件主题前缀
     */
    private String subjectPrefix = "[OSH QA Assistant]";
    /**
     * 告警发件人，未配置时默认回退到 spring.mail.username
     */
    private String from;
    /**
     * 接收告警的邮箱列表
     */
    private List<String> recipients = new ArrayList<>();
    /**
     * 同一类告警的最小重复发送间隔，单位：分钟
     */
    private long dedupeMinutes = 30;
    /**
     * 是否发送恢复通知
     */
    private boolean recoveryEnabled = true;
    /**
     * 是否允许人工自检触发告警演练
     */
    private boolean selfCheckEnabled = false;
    /**
     * 允许触发人工自检的用户名白名单
     */
    private List<String> selfCheckAllowedUsers = new ArrayList<>();
}
