package com.osh.ai.assistant.consumer.config;

import com.osh.ai.assistant.common.config.*;
import com.osh.ai.assistant.common.config.properties.AuthorizationProperties;
import com.osh.ai.assistant.common.config.properties.EnvProperties;
import com.osh.ai.assistant.consumer.config.properties.RagSplitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author zhaodaowen
 * @see <a href="https://example.invalid">项目维护者</a>
 */
@Configuration
@Import(value = {MybatisPlusConfig.class
        , ExHandlerConfig.class
        , RedisConfig.class
        , StorageConfig.class
        , WebMvcConfig.class
})
@EnableConfigurationProperties({AuthorizationProperties.class, EnvProperties.class, RagSplitProperties.class})
public class CommonConfig {

}
