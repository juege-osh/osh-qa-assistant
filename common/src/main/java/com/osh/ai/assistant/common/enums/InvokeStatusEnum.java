package com.osh.ai.assistant.common.enums;
import lombok.Getter;

/**
 * 调用记录或明细的状态枚举
 */
@Getter
public enum InvokeStatusEnum implements BaseEnum<Integer> {
    SUCCESS(1,"成功"),
    FAIL(0,"失败"),
    ;
    private final Integer code;
    private final String desc;
    InvokeStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
