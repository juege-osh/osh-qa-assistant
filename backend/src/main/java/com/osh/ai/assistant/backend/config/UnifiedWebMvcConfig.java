package com.osh.ai.assistant.backend.config;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.osh.ai.assistant.common.config.properties.AuthorizationProperties;
import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.interceptor.AuthorizationInterceptor;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class UnifiedWebMvcConfig implements WebMvcConfigurer {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);

    @Resource
    private AuthorizationProperties authorizationProperties;

    @Resource
    private UploadProperties uploadProperties;

    @Bean
    public AuthorizationInterceptor authorizationInterceptor() {
        return new AuthorizationInterceptor();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer objectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> {
            jacksonObjectMapperBuilder.serializerByType(Long.class, ToStringSerializer.instance);
            jacksonObjectMapperBuilder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            jacksonObjectMapperBuilder.simpleDateFormat(DatePattern.NORM_DATETIME_PATTERN);
            jacksonObjectMapperBuilder.serializerByType(
                LocalDateTime.class,
                new LocalDateTimeSerializer(DATE_TIME_FORMATTER)
            );
            jacksonObjectMapperBuilder.deserializerByType(
                LocalDateTime.class,
                new LocalDateTimeDeserializer(DATE_TIME_FORMATTER)
            );
        };
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
            "/manager",
            HandlerTypePredicate.forBasePackage("com.osh.ai.assistant.manager.controller")
        );
        configurer.addPathPrefix(
            "/consumer",
            HandlerTypePredicate.forBasePackage("com.osh.ai.assistant.consumer.controller")
        );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor())
            .addPathPatterns("/**")
            .excludePathPatterns(authorizationProperties.getWhiteList())
            .order(0);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedHeaders("*")
            .allowCredentials(true)
            .allowedMethods("*")
            .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseUploadDir = "file:" + uploadProperties.getStaticDir() + "resources/";
        registry.addResourceHandler("/resources/**").addResourceLocations(baseUploadDir);
        registry.addResourceHandler("/manager/resources/**").addResourceLocations(baseUploadDir);
        registry.addResourceHandler("/consumer/resources/**").addResourceLocations(baseUploadDir);
    }
}
