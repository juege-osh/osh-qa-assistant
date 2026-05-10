package com.osh.ai.assistant.common.enums;
import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
public enum ChatMessageTypeEnum implements BaseEnum<Integer> {
    USER(0,"user"),
    ASSISTANT(1,"assistant"),
    ;
    private final Integer code;
    private final String desc;
    ChatMessageTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
