package com.osh.ai.assistant.common.bean.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.osh.ai.assistant.common.enums.SexEnum;
import lombok.Data;

import java.util.Date;


@Data
@TableName("manager")
public class ManagerDO {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 密码
     */
    private String pwd;
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
}
