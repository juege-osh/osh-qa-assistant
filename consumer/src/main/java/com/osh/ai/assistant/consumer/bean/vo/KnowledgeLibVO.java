package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

import java.util.Date;
/**
  * 知识库视图对象
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
@Data
public class KnowledgeLibVO {
    /**
     * 主键
     */
    private Long id;
    /**
     * user表的主键
     */
    private Long userId;
    /**
     * 用户名
     */
    private String username;
    /**
     * app表的主键
     */
    private Long appId;
    /**
     * 应用名
     */
    private String appName;
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
    private Date createdTime;
    /**
     * 修改时间
     */
    private Date modifiedTime;
    /**
     * 文档数
     */
    private Long docCount;
    /**
     * 字符数
     */
    private Long charCount;
}
