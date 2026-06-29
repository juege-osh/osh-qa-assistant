package com.osh.ai.assistant.consumer.bean.req.app;

import com.osh.ai.assistant.common.enums.YesNoEnum;
import com.osh.ai.assistant.common.validator.EnumValue;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
/**
 * 应用信息更新入参类

 */
@Data
public class AppUpdateReq {
    /**
    * 主键
    */
    @NotNull
    private Long id;
    /**
    * 应用名称
    */
    @NotBlank
    private String appName;
    /**
    * 应用描述
    */
    @NotBlank
    private String appDesc;
    /**
    * 图标存放路径,格式如:resources/type/20230523/123.jpg
    */
    private String iconPath;
    /**
     * 超出知识库的问题是否回答
     * @see YesNoEnum
     */
    @NotNull
    @EnumValue(clazz = YesNoEnum.class)
    private Integer outLibEnable;
    private String customPrompt;
    private String chatModel;
}
