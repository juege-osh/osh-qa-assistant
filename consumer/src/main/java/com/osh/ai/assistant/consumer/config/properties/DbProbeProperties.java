package com.osh.ai.assistant.consumer.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai-assistant.db.probe")
public class DbProbeProperties {
    /**
     * 是否开启数据库启动探针
     */
    private boolean enabled = true;
    /**
     * 超过多少秒仍不可用后开始告警
     */
    private long warnAfterSeconds = 10;
    /**
     * 每次重试间隔，单位：秒
     */
    private long retryIntervalSeconds = 10;
    /**
     * 单次连接超时秒数
     */
    private int checkTimeoutSeconds = 5;
}
