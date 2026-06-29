package com.osh.ai.assistant.common.bean.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 知识库

 */
@Data
@TableName("knowledge_lib")
public class KnowledgeLibDO {
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
     * 知识库名称
     */
    private String libName;
    /**
     * 知识库描述
     */
    private String libDesc;
    /**
     * 图标存放路径,格式如:resources/type/20230523/123.jpg
     */
    private String iconPath;
    /**
     * 当前生效切分策略
     */
    private String activeSplitStrategy;
    /**
     * 当前生效切分参数快照
     */
    private String activeSplitConfigJson;
    /**
     * 当前生效实验版本id
     */
    private Long activeExperimentId;
    /**
     * 当前生效实验版本名称
     */
    private String activeExperimentName;
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
