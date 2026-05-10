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
