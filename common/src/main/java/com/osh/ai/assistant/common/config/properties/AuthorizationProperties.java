package com.osh.ai.assistant.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;


@Data
@ConfigurationProperties(prefix = "auth")
public class AuthorizationProperties {
    /**
     * 不需要拦截器拦截的url列表
     */
    private List<String> whiteList;
}
