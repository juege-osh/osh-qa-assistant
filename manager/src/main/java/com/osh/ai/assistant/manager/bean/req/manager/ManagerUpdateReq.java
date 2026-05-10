package com.osh.ai.assistant.manager.bean.req.manager;

import com.osh.ai.assistant.common.enums.SexEnum;
import com.osh.ai.assistant.common.validator.EnumValue;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 个人信息修改请求参数
 */
@Data
public class ManagerUpdateReq {
    /**
     * 主键
     */
    @NotNull
    private Long id;
    /**
     * 姓名
     */
    @NotBlank
    private String realName;
    /**
     * @see SexEnum
     */
    @NotNull
    @EnumValue(clazz = SexEnum.class)
    private Integer sex;
    /**
     * 头像存放路径,格式如:resources/type/20230523/123.jpg
     */
    private String avatarPath;
}
