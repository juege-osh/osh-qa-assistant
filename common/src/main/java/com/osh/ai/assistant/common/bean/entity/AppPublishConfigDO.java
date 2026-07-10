package com.osh.ai.assistant.common.bean.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 应用公开发布配置
 */
@Data
@TableName("app_publish_config")
public class AppPublishConfigDO {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * app表主键
     */
    private Long appId;
    /**
     * 是否启用公开访问,1:是,0:否
     */
    private Integer enabled;
    /**
     * 公开访问标识
     */
    private String slug;
    /**
     * 访问方式:PUBLIC/PASSWORD
     */
    private String accessType;
    /**
     * 访问密码哈希
     */
    private String passwordHash;
    /**
     * 自定义访问域名
     */
    private String customDomain;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createdTime;
    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date modifiedTime;
}
