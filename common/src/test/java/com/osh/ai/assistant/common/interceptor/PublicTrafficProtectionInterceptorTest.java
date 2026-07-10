package com.osh.ai.assistant.common.interceptor;

import com.osh.ai.assistant.common.config.properties.PublicTrafficProtectionProperties;
import com.osh.ai.assistant.common.enums.CodeEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.ex.RateLimitEx;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PublicTrafficProtectionInterceptorTest {

    @Test
    void shouldBlockAfterPermitsExceeded() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-09T00:00:00Z"));
        PublicTrafficProtectionInterceptor interceptor = new PublicTrafficProtectionInterceptor(buildLoginProtection(), clock);

        assertTrue(interceptor.preHandle(buildRequest("POST", "/auth/login", "10.0.0.1"),
            new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(buildRequest("POST", "/auth/login", "10.0.0.1"),
            new MockHttpServletResponse(), new Object()));

        try {
            interceptor.preHandle(buildRequest("POST", "/auth/login", "10.0.0.1"),
                new MockHttpServletResponse(), new Object());
            fail("expected rate limit rejection");
        } catch (RateLimitEx e) {
            assertEquals(CodeEnum.RATE_LIMIT_ERR.getCode(), e.getCode());
            assertEquals("登录太频繁", e.getMessage());
            assertEquals(60L, e.getRetryAfterSeconds());
        }
    }

    @Test
    void shouldUseForwardedIpAsClientKey() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-09T00:00:00Z"));
        PublicTrafficProtectionInterceptor interceptor = new PublicTrafficProtectionInterceptor(buildCaptchaProtection(), clock);

        MockHttpServletRequest first = buildRequest("GET", "/auth/getCode", "127.0.0.1");
        first.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.2");
        MockHttpServletRequest second = buildRequest("GET", "/auth/getCode", "127.0.0.1");
        second.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.3");

        assertTrue(interceptor.preHandle(first, new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(second, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void shouldIgnoreForwardedHeaderFromUntrustedDirectClient() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-09T00:00:00Z"));
        PublicTrafficProtectionInterceptor interceptor = new PublicTrafficProtectionInterceptor(buildCaptchaProtection(), clock);

        MockHttpServletRequest first = buildRequest("GET", "/auth/getCode", "198.51.100.10");
        first.addHeader("X-Forwarded-For", "203.0.113.9");
        MockHttpServletRequest second = buildRequest("GET", "/auth/getCode", "198.51.100.10");
        second.addHeader("X-Forwarded-For", "203.0.113.11");

        assertTrue(interceptor.preHandle(first, new MockHttpServletResponse(), new Object()));

        try {
            interceptor.preHandle(second, new MockHttpServletResponse(), new Object());
            fail("expected direct client to be limited by remote address");
        } catch (RateLimitEx e) {
            assertEquals(CodeEnum.RATE_LIMIT_ERR.getCode(), e.getCode());
            assertEquals(60L, e.getRetryAfterSeconds());
        }
    }

    @Test
    void shouldResetWindowAfterExpiration() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-09T00:00:00Z"));
        PublicTrafficProtectionInterceptor interceptor = new PublicTrafficProtectionInterceptor(buildLoginProtection(), clock);

        assertTrue(interceptor.preHandle(buildRequest("POST", "/auth/login", "10.0.0.8"),
            new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(buildRequest("POST", "/auth/login", "10.0.0.8"),
            new MockHttpServletResponse(), new Object()));

        clock.plusSeconds(61);

        assertTrue(interceptor.preHandle(buildRequest("POST", "/auth/login", "10.0.0.8"),
            new MockHttpServletResponse(), new Object()));
    }

    @Test
    void shouldIgnoreNonProtectedPath() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-09T00:00:00Z"));
        PublicTrafficProtectionInterceptor interceptor = new PublicTrafficProtectionInterceptor(buildLoginProtection(), clock);

        for (int i = 0; i < 5; i++) {
            assertTrue(interceptor.preHandle(buildRequest("GET", "/consumer/app/queryPage", "10.0.0.20"),
                new MockHttpServletResponse(), new Object()));
        }
    }

    private PublicTrafficProtectionProperties buildLoginProtection() {
        PublicTrafficProtectionProperties properties = new PublicTrafficProtectionProperties();
        PublicTrafficProtectionProperties.Rule rule = new PublicTrafficProtectionProperties.Rule();
        rule.setName("login");
        rule.setPathPattern("/auth/login");
        rule.setMethod("POST");
        rule.setPermits(2);
        rule.setWindowSeconds(60);
        rule.setMessage("登录太频繁");
        properties.getRules().add(rule);
        return properties;
    }

    private PublicTrafficProtectionProperties buildCaptchaProtection() {
        PublicTrafficProtectionProperties properties = new PublicTrafficProtectionProperties();
        PublicTrafficProtectionProperties.Rule rule = new PublicTrafficProtectionProperties.Rule();
        rule.setName("captcha");
        rule.setPathPattern("/auth/getCode");
        rule.setMethod("GET");
        rule.setPermits(1);
        rule.setWindowSeconds(60);
        properties.getRules().add(rule);
        return properties;
    }

    private MockHttpServletRequest buildRequest(String method, String servletPath, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, servletPath);
        request.setServletPath(servletPath);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private static class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void plusSeconds(long seconds) {
            current = current.plusSeconds(seconds);
        }
    }
}
