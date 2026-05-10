package com.osh.ai.assistant.common.enums;
import lombok.Getter;

/**
 * 上传的文件状态枚举
 */
@Getter
public enum UploadFileStatusEnum implements BaseEnum<Integer> {
    DISABLED(0,"禁用"),
    ENABLED(1,"启用"),
    ;
    private final Integer code;
    private final String desc;
    UploadFileStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
