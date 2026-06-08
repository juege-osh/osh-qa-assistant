package com.osh.ai.assistant.common.enums;

import lombok.Getter;

@Getter
public enum AccountRoleEnum {
    ADMIN("ADMIN", "管理员"),
    USER("USER", "普通用户");

    private final String code;
    private final String desc;

    AccountRoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AccountRoleEnum of(String code) {
        for (AccountRoleEnum role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        return null;
    }
}
