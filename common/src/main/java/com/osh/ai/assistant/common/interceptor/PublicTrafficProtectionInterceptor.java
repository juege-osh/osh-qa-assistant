package com.osh.ai.assistant.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.osh.ai.assistant.common.config.properties.PublicTrafficProtectionProperties;
import com.osh.ai.assistant.common.enums.CodeEnum;
import com.osh.ai.assistant.common.ex.RateLimitEx;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * 针对白名单公网入口的轻量级限流保护。
 * 目前采用单机内存固定窗口计数，优先覆盖验证码、登录、注册、公开聊天等高风险入口。
 */
@Slf4j
public class PublicTrafficProtectionInterceptor implements HandlerInterceptor {
    private static final String UNKNOWN_IP = "unknown";

    private final PublicTrafficProtectionProperties properties;
    private final Cache<String, WindowCounter> counters;
    private final PathMatcher pathMatcher = new AntPathMatcher();
    private final Clock clock;

    public PublicTrafficProtectionInterceptor(PublicTrafficProtectionProperties properties) {
        this(properties, Clock.systemUTC());
    }

    PublicTrafficProtectionInterceptor(PublicTrafficProtectionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        long maxTrackedClients = Math.max(1_000L, properties.getMaxTrackedClients());
        this.counters = CacheBuilder.newBuilder()
            .maximumSize(maxTrackedClients)
            .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled() || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        PublicTrafficProtectionProperties.Rule rule = matchRule(request.getServletPath(), request.getMethod());
        if (rule == null) {
            return true;
        }

        String clientIp = resolveClientIp(request);
        String ruleName = resolveRuleName(rule);
        String counterKey = ruleName + "|" + clientIp;
        long now = clock.millis();
        WindowCounter counter;
        try {
            counter = counters.get(counterKey, WindowCounter::new);
        } catch (ExecutionException e) {
            log.warn("load rate-limit counter failed, path={}, method={}, clientIp={}",
                request.getServletPath(), request.getMethod(), clientIp, e);
            return true;
        }

        int permits = Math.max(1, rule.getPermits());
        long windowMillis = Math.max(1, rule.getWindowSeconds()) * 1000L;
        WindowDecision decision = counter.tryAcquire(now, permits, windowMillis);
        if (decision.allowed()) {
            return true;
        }

        log.warn("public traffic limited, rule={}, method={}, path={}, clientIp={}, permits={}, windowSeconds={}",
            ruleName, request.getMethod(), request.getServletPath(), clientIp, permits, rule.getWindowSeconds());
        throw new RateLimitEx(resolveMessage(rule), decision.retryAfterSeconds());
    }

    private PublicTrafficProtectionProperties.Rule matchRule(String servletPath, String requestMethod) {
        if (properties.getRules() == null || properties.getRules().isEmpty()) {
            return null;
        }
        for (PublicTrafficProtectionProperties.Rule rule : properties.getRules()) {
            if (rule == null || StrUtil.isBlank(rule.getPathPattern())) {
                continue;
            }
            boolean methodMatched = StrUtil.isBlank(rule.getMethod())
                || StrUtil.equalsIgnoreCase(rule.getMethod(), requestMethod);
            if (methodMatched && pathMatcher.match(rule.getPathPattern(), servletPath)) {
                return rule;
            }
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = normalizeIp(request.getRemoteAddr());
        if (properties.isTrustForwardHeaders() && isTrustedProxy(remoteAddr)) {
            String forwardedFor = firstForwardedIp(request.getHeader("X-Forwarded-For"));
            if (StrUtil.isNotBlank(forwardedFor)) {
                return forwardedFor;
            }
            String realIp = normalizeIp(request.getHeader("X-Real-IP"));
            if (StrUtil.isNotBlank(realIp)) {
                return realIp;
            }
        }
        return remoteAddr;
    }

    private String firstForwardedIp(String headerValue) {
        if (StrUtil.isBlank(headerValue)) {
            return null;
        }
        String[] ipList = headerValue.split(",");
        for (String ip : ipList) {
            String normalized = normalizeIp(ip);
            if (StrUtil.isNotBlank(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return "unknown-client";
        }
        String normalized = ip.trim();
        if (UNKNOWN_IP.equalsIgnoreCase(normalized)) {
            return "unknown-client";
        }
        return normalized;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if ("unknown-client".equals(remoteAddr)) {
            return false;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(remoteAddr);
            return inetAddress.isAnyLocalAddress()
                || inetAddress.isLoopbackAddress()
                || inetAddress.isLinkLocalAddress()
                || inetAddress.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            log.debug("parse remote address failed, fallback to direct ip, remoteAddr={}", remoteAddr, e);
            return false;
        }
    }

    private String resolveRuleName(PublicTrafficProtectionProperties.Rule rule) {
        if (StrUtil.isNotBlank(rule.getName())) {
            return rule.getName().trim();
        }
        String method = StrUtil.blankToDefault(rule.getMethod(), "ALL").toUpperCase(Locale.ROOT);
        return method + ":" + rule.getPathPattern();
    }

    private String resolveMessage(PublicTrafficProtectionProperties.Rule rule) {
        return StrUtil.blankToDefault(rule.getMessage(), CodeEnum.RATE_LIMIT_ERR.getDesc()).trim();
    }

    private static class WindowCounter {
        private long windowStartedAtMillis;
        private int usedPermits;

        synchronized WindowDecision tryAcquire(long nowMillis, int maxPermits, long windowMillis) {
            if (windowStartedAtMillis <= 0 || nowMillis - windowStartedAtMillis >= windowMillis) {
                windowStartedAtMillis = nowMillis;
                usedPermits = 0;
            }
            if (usedPermits >= maxPermits) {
                long remainingMillis = Math.max(1L, windowMillis - (nowMillis - windowStartedAtMillis));
                long retryAfterSeconds = (remainingMillis + 999L) / 1000L;
                return new WindowDecision(false, retryAfterSeconds);
            }
            usedPermits++;
            return new WindowDecision(true, 0L);
        }
    }

    private record WindowDecision(boolean allowed, long retryAfterSeconds) {
    }
}
