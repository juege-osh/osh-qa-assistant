package com.osh.ai.assistant.common.ex.handler;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.ex.RateLimitEx;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExHandlerTest {

    @Test
    void shouldReturn429WithRetryMetadataForRateLimit() {
        ExHandler handler = new ExHandler();

        ResponseEntity<Result<String>> response = handler.handleEx(new RateLimitEx("请求过于频繁，请稍后再试", 27));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("27", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        Result<String> body = response.getBody();
        assertNotNull(body);
        assertEquals(30002, body.getCode());
        assertEquals("请求过于频繁，请稍后再试", body.getMsg());
        assertNotNull(body.getExtra());
        assertEquals("RATE_LIMIT", body.getExtra().get("errorType"));
        assertEquals(Boolean.TRUE, body.getExtra().get("retryable"));
        assertEquals(27L, body.getExtra().get("retryAfterSeconds"));
        assertTrue(String.valueOf(body.getExtra().get("suggestion")).contains("27"));
    }
}
