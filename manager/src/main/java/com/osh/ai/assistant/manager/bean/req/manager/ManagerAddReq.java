package com.osh.ai.assistant.manager.bean.req.manager;

import com.osh.ai.assistant.common.enums.SexEnum;
import com.osh.ai.assistant.common.validator.EnumValue;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 管理员新增入参类
 */
@Data
public class ManagerAddReq {
    /**
     * 用户名
     */
    @NotBlank
    private String username;
    /**
     * 姓名
     */
    @NotBlank
    private String realName;
    /**
     * 密码
     */
    @NotBlank
    private String pwd;
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
