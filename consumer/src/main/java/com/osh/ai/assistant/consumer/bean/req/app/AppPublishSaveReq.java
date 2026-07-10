package com.osh.ai.assistant.consumer.bean.req.app;

import com.osh.ai.assistant.common.enums.AppPublishAccessTypeEnum;
import com.osh.ai.assistant.common.enums.YesNoEnum;
import com.osh.ai.assistant.common.validator.EnumValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 应用公开发布配置保存入参
 */
@Data
public class AppPublishSaveReq {
    /**
     * 应用id
     */
    @NotNull
    private Long appId;
    /**
     * 是否启用公开访问
     */
    @NotNull
    @EnumValue(clazz = YesNoEnum.class)
    private Integer enabled;
    /**
     * 公开访问标识
     */
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_-]{2,99}$", message = "公开链接标识只能包含字母、数字、下划线和中划线，长度为3-100位")
    private String slug;
    /**
     * 访问方式
     */
    @NotBlank
    @EnumValue(clazz = AppPublishAccessTypeEnum.class)
    private String accessType;
    /**
     * 访问密码。为空时保留原密码，公开访问时会清空密码。
     */
    private String accessPassword;
}
