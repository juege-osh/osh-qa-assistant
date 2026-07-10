package com.osh.ai.assistant.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "ai-assistant.public-traffic-protection")
public class PublicTrafficProtectionProperties {
    /**
     * 是否启用公网入口流量保护。
     */
    private boolean enabled = true;

    /**
     * 是否优先信任代理透传的客户端 IP 头。
     */
    private boolean trustForwardHeaders = true;

    /**
     * 内存中最多保留多少个客户端限流桶。
     */
    private long maxTrackedClients = 100_000L;

    /**
     * 需要限流的公网规则列表。
     */
    private List<Rule> rules = new ArrayList<>();

    @Data
    public static class Rule {
        /**
         * 规则名称，用于日志与缓存 key。
         */
        private String name;

        /**
         * 需要匹配的路径模式。
         */
        private String pathPattern;

        /**
         * HTTP 方法，为空时表示匹配全部方法。
         */
        private String method;

        /**
         * 固定窗口内允许的最大请求数。
         */
        private int permits = 60;

        /**
         * 固定窗口大小，单位秒。
         */
        private int windowSeconds = 60;

        /**
         * 超限后的返回提示。
         */
        private String message = "请求过于频繁，请稍后再试";
    }
}
