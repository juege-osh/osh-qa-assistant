package com.osh.ai.assistant.common.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * OSS 上传路径类型，保留原 osh-backend 的业务目录约定。
 */
@Getter
public enum UploadPathEnum {

    AVATAR("common/image/avatar/"),
    COURSE_VIDEO("common/video/course/"),
    COURSE_COVER("common/image/courseCover/"),
    DOCUMENT("common/document/"),
    COURSE_MATERIAL("common/material/course/"),
    TOOL_COVER("common/image/toolCover/"),
    AUDIO("common/audio/"),
    IMAGE("common/image/"),
    TEMP("temp/"),
    INNER_SITE("site/"),
    RESOURCE("resources/"),
    DEFAULT("common/");

    private final String path;

    UploadPathEnum(String path) {
        this.path = path;
    }

    /**
     * 将前端传入的 module 映射为固定 OSS 目录，未知模块回退到 resources，
     * 避免把任意用户输入直接作为对象 key 前缀。
     */
    public static UploadPathEnum fromModule(String module) {
        if (StringUtils.isBlank(module)) {
            return RESOURCE;
        }
        for (UploadPathEnum uploadPathEnum : values()) {
            if (StringUtils.equalsIgnoreCase(uploadPathEnum.name(), module)
                    || StringUtils.equals(uploadPathEnum.path, normalizeModule(module))) {
                return uploadPathEnum;
            }
        }
        return RESOURCE;
    }

    private static String normalizeModule(String module) {
        String normalizedModule = StringUtils.trimToEmpty(module);
        return normalizedModule.endsWith("/") ? normalizedModule : normalizedModule + "/";
    }
}
