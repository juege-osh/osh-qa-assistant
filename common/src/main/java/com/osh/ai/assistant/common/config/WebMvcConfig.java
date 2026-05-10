package com.osh.ai.assistant.common.config;

import cn.hutool.core.date.DatePattern;
import com.osh.ai.assistant.common.config.properties.AuthorizationProperties;
import com.osh.ai.assistant.common.controller.HealthController;
import com.osh.ai.assistant.common.interceptor.AuthorizationInterceptor;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 自定义的webmvc配置
 */
public class WebMvcConfig implements WebMvcConfigurer {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);
    @Resource
    private AuthorizationProperties authorizationProperties;

    @Bean
    public AuthorizationInterceptor authorizationInterceptor() {
        return new AuthorizationInterceptor();
    }

    /**
     * 配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor())
            .addPathPatterns("/**")
            // 排除哪些路径
            .excludePathPatterns(authorizationProperties.getWhiteList())
            // 值越小,优先级越高
            .order(0);
    }
    /**
     * 定制化ObjectMapper
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer objectMapperBuilderCustomizer() {
       return  jacksonObjectMapperBuilder -> {
           // Long类型序列化为字符串,避免精度丢失
           jacksonObjectMapperBuilder.serializerByType(Long.class, ToStringSerializer.instance);
           // long类型序列化为字符串,避免精度丢失
           jacksonObjectMapperBuilder.serializerByType(Long.TYPE, ToStringSerializer.instance);
           // date类型格式化
           jacksonObjectMapperBuilder.simpleDateFormat(DatePattern.NORM_DATETIME_PATTERN);
           //LocalDateTime类型格式化
           jacksonObjectMapperBuilder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
           jacksonObjectMapperBuilder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
       };
    }
    @Bean
    public HealthController healthController() {
        return new HealthController();
    }
    /**
     * 配置允许跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
            // 允许跨域访问的url
            .addMapping("/**")
            // 允许跨域的域名
            .allowedOriginPatterns("*")
            .allowedHeaders("*")
            .allowCredentials(true)
            // get,options,post...
            .allowedMethods("*")
            // 单位:s,用来设置options请求(预检请求)在客户端的缓存时间
            .maxAge(3600);
    }
}
