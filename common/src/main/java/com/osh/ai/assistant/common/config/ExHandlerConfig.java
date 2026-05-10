package com.osh.ai.assistant.common.config;

import com.osh.ai.assistant.common.ex.handler.ExHandler;
import org.springframework.context.annotation.Bean;

/**
 * @author zhaodaowen
 * @see <a href="https://example.invalid">项目维护者</a>
 */
public class ExHandlerConfig {
    @Bean
    public ExHandler controllerExHandler() {
        return new ExHandler();
    }
}
