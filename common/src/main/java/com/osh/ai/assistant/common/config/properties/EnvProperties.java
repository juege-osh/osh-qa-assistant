package com.osh.ai.assistant.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**

 */
@ConfigurationProperties(prefix = "env")
@Data
public class EnvProperties {
    /**
     * 请求后端资源基础路径
     */
    private String baseResourceUrl;

}
