package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

@Data
public class RagSplitConfigVO {
    /**
     * 切分策略
     */
    private String strategy;
    /**
     * 目标 chunk token 大小
     */
    private Integer chunkSize;
    /**
     * 最小 chunk token 大小
     */
    private Integer minChunkSizeChars;
    /**
     * 最小保留段落长度
     */
    private Integer minChunkLengthToEmbed;
    /**
     * 最大切分 chunk 数
     */
    private Integer maxNumChunks;
    /**
     * 是否保留分隔符
     */
    private Boolean keepSeparator;
    /**
     * 语义分段的最大字符数
     */
    private Integer semanticSectionMaxChars;
    /**
     * 预览时返回的 chunk 数
     */
    private Integer previewChunkLimit;
}
