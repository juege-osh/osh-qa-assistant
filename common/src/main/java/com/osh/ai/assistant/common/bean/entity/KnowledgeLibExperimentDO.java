package com.osh.ai.assistant.common.bean.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("knowledge_lib_experiment")
public class KnowledgeLibExperimentDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * knowledge_lib 主键
     */
    private Long libId;
    /**
     * 实验版本名称
     */
    private String versionName;
    /**
     * 调试问题
     */
    private String queryText;
    /**
     * 切分策略
     */
    private String splitStrategy;
    /**
     * 原始召回数
     */
    private Integer rawHitCount;
    /**
     * 重排后召回数
     */
    private Integer rerankHitCount;
    /**
     * 诊断结论
     */
    private String diagnosisTitle;
    /**
     * 归类编码
     */
    private String categoryCode;
    /**
     * 归类名称
     */
    private String categoryLabel;
    /**
     * 原始 top1 摘要
     */
    private String rawTopSummary;
    /**
     * 重排 top1 摘要
     */
    private String rerankTopSummary;
    /**
     * 是否推荐
     */
    private Integer recommended;
    @TableField(fill = FieldFill.INSERT)
    private Date createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date modifiedTime;
}
