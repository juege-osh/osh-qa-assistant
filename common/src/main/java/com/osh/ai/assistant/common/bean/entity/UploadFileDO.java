package com.osh.ai.assistant.common.bean.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 文件信息

 */
@Data
@TableName("upload_file")
public class UploadFileDO {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * knowledge_base表的主键
     */
    private Long libId;
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 文件存放路径,格式如:resources/type/20230523/123.jpg
     */
    private String storePath;
    /**
     * 字符数
     */
    private Long charCount;
    /**
     * 召回次数
     */
    private Long recallCount;
    /**
     * 状态,0:禁用,1:启用
     */
    private Integer status;
    /**
     * 关联的文档id列表,json数组
     */
    private String docIds;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createdTime;
}
