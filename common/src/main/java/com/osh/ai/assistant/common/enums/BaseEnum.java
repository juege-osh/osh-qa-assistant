package com.osh.ai.assistant.common.enums;
import lombok.Getter;
/**
 * 枚举类顶级接口
 */
public interface BaseEnum<E> {
    E getCode();
    String getDesc();
}
