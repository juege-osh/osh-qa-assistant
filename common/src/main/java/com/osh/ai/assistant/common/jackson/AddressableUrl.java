package com.osh.ai.assistant.common.jackson;

import com.osh.ai.assistant.common.config.properties.EnvProperties;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在字段上用于把相对路径拼接上
 * {@link EnvProperties#getBaseResourceUrl()}转为可访问地址

 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
// 让jackson能够解析自定义注解上的其他jackson注解(此处即@JsonSerialize)
@JacksonAnnotationsInside
// 指定序列化的实现类
@JsonSerialize(using = AddressableUrlSerializer.class)
public @interface AddressableUrl {
}
