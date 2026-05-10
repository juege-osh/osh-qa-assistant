package com.osh.ai.assistant.backend;

import com.osh.ai.assistant.common.config.ExHandlerConfig;
import com.osh.ai.assistant.common.config.MybatisPlusConfig;
import com.osh.ai.assistant.common.config.RedisConfig;
import com.osh.ai.assistant.common.config.properties.AuthorizationProperties;
import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.controller.StorageController;
import com.osh.ai.assistant.consumer.ConsumerApp;
import com.osh.ai.assistant.manager.ManagerApp;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAutoConfiguration
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableConfigurationProperties({AuthorizationProperties.class, UploadProperties.class})
@Import({MybatisPlusConfig.class, RedisConfig.class, ExHandlerConfig.class})
@ComponentScan(
    basePackages = {
        "com.osh.ai.assistant.backend",
        "com.osh.ai.assistant.common",
        "com.osh.ai.assistant.manager",
        "com.osh.ai.assistant.consumer"
    },
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ManagerApp.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ConsumerApp.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.osh.ai.assistant.manager.config.CommonConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.osh.ai.assistant.consumer.config.CommonConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = StorageController.class)
    }
)
@MapperScan(
    basePackages = {
        "com.osh.ai.assistant.manager.mapper",
        "com.osh.ai.assistant.consumer.mapper"
    },
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class BackendApp {

    public static void main(String[] args) {
        SpringApplication.run(BackendApp.class, args);
    }
}
