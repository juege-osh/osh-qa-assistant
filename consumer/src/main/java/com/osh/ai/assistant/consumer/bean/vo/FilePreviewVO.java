package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

import java.util.List;

@Data
public class FilePreviewVO {
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 文件字符数
     */
    private Long charCount;
    /**
     * 文件状态
     */
    private Integer status;
    /**
     * 文件状态描述
     */
    private String statusDesc;
    /**
     * 原文预览
     */
    private String content;
    /**
     * 切分后的总 chunk 数
     */
    private Integer chunkCount;
    /**
     * 当前切分配置
     */
    private RagSplitConfigVO splitConfig;
    /**
     * 预览 chunk 列表
     */
    private List<RagSplitChunkVO> chunks;
}
