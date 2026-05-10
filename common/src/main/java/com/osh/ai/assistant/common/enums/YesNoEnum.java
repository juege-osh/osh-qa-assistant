package com.osh.ai.assistant.common.enums;
import lombok.Getter;
/**
 * 是否枚举
 */
@Getter
public enum YesNoEnum implements BaseEnum<Integer> {
    Y(1,"是"),
    N(0,"否"),
    ;
    private final Integer code;
    private final String desc;
    YesNoEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
