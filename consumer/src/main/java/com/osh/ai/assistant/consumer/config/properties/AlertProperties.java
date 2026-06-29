package com.osh.ai.assistant.consumer.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
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
}
