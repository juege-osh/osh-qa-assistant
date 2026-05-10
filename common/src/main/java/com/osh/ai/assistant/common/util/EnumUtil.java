package com.osh.ai.assistant.common.util;


import com.osh.ai.assistant.common.enums.BaseEnum;

import java.util.Objects;

/**
 * 枚举类型工具类
 *
 */
public class EnumUtil {

    /**
     * 通过code获取对应的枚举对象
     * 通过 code 获取对应枚举对象
     * @param code       编码值
     * @param enumClass  枚举类型
     * @param <E>        编码类型
     * @param <T>        枚举类型
     * @return 不存在返回 null
     */
    public static <E,T extends BaseEnum<E>> T getByCode(E code, Class<T> enumClass) {
        if(code == null || enumClass == null) {
            return null;
        }
        for (T each : enumClass.getEnumConstants()) {
            if (Objects.equals(each.getCode(),code)) {
                return each;
            }
        }
        return null;
    }
    /**
     * 通过code获取对应的描述
     * @return 不存在则返回null
     */
    public static <E,T extends BaseEnum<E>> String getDescByCode(E code, Class<T> enumClass) {
        T e = getByCode(code, enumClass);
        return e != null ? e.getDesc() : null;
    }
}
