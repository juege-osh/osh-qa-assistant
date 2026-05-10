package com.osh.ai.assistant.common.validator;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 自定义校验器,校验枚举值是否存在
 */
@Documented
@Constraint(validatedBy = { EnumValueValidator.class}) // 说明当前注解要被谁来完成校验工作
@Target({ FIELD})
@Retention(RUNTIME)
public @interface EnumValue {

    String message() default "枚举值不合法";

    /**
     * 即便在XxxValidator中没使用到也需要定义
     */
    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    /**
     * 指定从哪个枚举类里面取值用作校验源
     */
    Class<? extends Enum<?>> clazz();

    /**
     * 从枚举类里取哪个字段的值
     */
    String field() default "code";
}
