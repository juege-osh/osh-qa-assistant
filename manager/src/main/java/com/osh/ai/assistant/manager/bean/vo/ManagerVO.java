package com.osh.ai.assistant.manager.bean.vo;

import com.osh.ai.assistant.common.enums.SexEnum;
import com.osh.ai.assistant.common.util.EnumUtil;
import lombok.Data;

import java.util.Date;


@Data
public class ManagerVO {
    /**
     * 主键
     */
    private Long id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 姓名
     */
    private String realName;
    /**
     * @see SexEnum
     */
    private Integer sex;
    /**
     * 头像存放路径,格式如:resources/type/20230523/123.jpg
     */
    private String avatarPath;
    /**
     * 上次登录时间
     */
    private Date lastLoginTime;
    /**
     * {@link #sex}对应的描述
     */
    private String sexDesc;

    public String getSexDesc() {
        return EnumUtil.getDescByCode(sex, SexEnum.class);
    }
}
