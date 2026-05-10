package com.osh.ai.assistant.common.enums;
import lombok.Getter;
/**
 * 响应码枚举类
 */
@Getter
public enum  CodeEnum implements BaseEnum<Integer> {
    SUCCESS(200,"成功"),

    PARAM_ERR(9000,"参数不正确"),
    ERR_9001(9001,"appKey不存在"),

    DB_ERR(20000,"数据库异常"),
    DUPLICATE_KEY_ERR(20001,"唯一性约束校验失败"),

    BIZ_ERR(30000,"业务异常"),
    AUTH_ERR(30001,"权限不足"),

    SYS_ERR(90000,"系统异常"),
    ;
    CodeEnum(Integer code,String desc) {
        this.code = code;
        this.desc = desc;
    }
    private final Integer code;
    private final String desc;
}
