package com.osh.ai.assistant.common.enums;

import lombok.Getter;

/**
 * 应用公开访问方式
 */
@Getter
public enum AppPublishAccessTypeEnum implements BaseEnum<String> {
    PUBLIC("PUBLIC", "公开访问"),
    PASSWORD("PASSWORD", "密码访问");

    private final String code;
    private final String desc;

    AppPublishAccessTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
