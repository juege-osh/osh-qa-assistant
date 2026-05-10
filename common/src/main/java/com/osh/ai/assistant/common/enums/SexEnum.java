package com.osh.ai.assistant.common.enums;
import lombok.Getter;
/**
 * 性别枚举
 */
@Getter
public enum SexEnum implements BaseEnum<Integer> {
    MALE(1,"男"),
    FEMALE(0,"女");

    private final Integer code;
    private final String desc;
    SexEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
