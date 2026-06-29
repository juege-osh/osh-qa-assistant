package com.osh.ai.assistant.common.bean.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.osh.ai.assistant.common.enums.YesNoEnum;
import lombok.Data;

import java.util.Date;

/**
 * 应用信息

 */
@Data
@TableName("app")
public class AppDO {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * user表的主键
     */
    private Long userId;
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 应用描述
     */
    private String appDesc;
    /**
     * 图标存放路径,格式如:resources/type/20230523/123.jpg
     */
    private String iconPath;
    /**
     * 超出知识库的问题是否回答
     * @see YesNoEnum
     */
    private Integer outLibEnable;
    /**
     * 应用级自定义系统提示词
     */
    private String customPrompt;
    /**
     * 应用级聊天模型名称
     */
    private String chatModel;
    /**
     * 关联的知识库id
     */
    private Long libId;
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
