package com.osh.ai.assistant.common.validator;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * 每个字段上的注解都对应一个对象,所以不会有线程安全问题
 * 泛型Object:表示通过{@link EnumValue#field()}指定的字段的类型,这里用Object表示
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, Object> {

    private Set<Object> set;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        // 指定的枚举类
        Class<? extends Enum<?>> clazz = constraintAnnotation.clazz();
        // 获取所有枚举对象
        Enum<?>[] enumConstants = clazz.getEnumConstants();
        if (enumConstants == null) {
            return;
        }
        set = new HashSet<>();
        for (Enum<?> enumConstant : enumConstants) {
            Field field = ReflectionUtils.findField(clazz, constraintAnnotation.field());
            if (field == null) {
                continue;
            }
            ReflectionUtils.makeAccessible(field);
            Object fieldValue = ReflectionUtils.getField(field, enumConstant);
            set.add(fieldValue);
        }
    }

    /**
     * @param value 用户填写的待校验入参
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null || CollectionUtils.isEmpty(set)) {
            return true;
        }
        if (value instanceof String) {
            if (StrUtil.isBlank(value.toString())) {
                return true;
            }
        }
        return set.contains(value);
    }
}
