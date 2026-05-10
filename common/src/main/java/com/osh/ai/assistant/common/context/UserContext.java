package com.osh.ai.assistant.common.context;


import com.osh.ai.assistant.common.bean.dto.TokenDTO;
import com.osh.ai.assistant.common.ex.BizEx;


public class UserContext {
    private static final ThreadLocal<TokenDTO> THREAD_LOCAL = new ThreadLocal<>();
    public static void set(TokenDTO dto) {
        THREAD_LOCAL.set(dto);
    }
    public static TokenDTO get() {
        return THREAD_LOCAL.get();
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    public static Long getUserId() {
        TokenDTO dto = get();
        if (dto == null) {
            throw new BizEx("用户未登录");
        }
        return dto.getId();
    }
}
