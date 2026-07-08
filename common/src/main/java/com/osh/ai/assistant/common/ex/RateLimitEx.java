package com.osh.ai.assistant.common.ex;

import com.osh.ai.assistant.common.enums.CodeEnum;
import lombok.Getter;

@Getter
public class RateLimitEx extends BizEx {
    private final long retryAfterSeconds;

    public RateLimitEx(String msg, long retryAfterSeconds) {
        super(CodeEnum.RATE_LIMIT_ERR.getCode(), msg);
        this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
    }
}
