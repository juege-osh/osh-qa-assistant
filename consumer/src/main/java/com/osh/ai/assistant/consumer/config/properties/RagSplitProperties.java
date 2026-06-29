package com.osh.ai.assistant.consumer.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai-assistant.rag.split")
public class RagSplitProperties {
    /**
     * 切分策略：token / semantic
     */
    private String strategy = "semantic";
    /**
     * 目标 chunk token 大小
     */
    private int chunkSize = 800;
    /**
     * 最小 chunk token 大小
     */
    private int minChunkSizeChars = 350;
    /**
     * 最小保留段落长度
     */
    private int minChunkLengthToEmbed = 5;
    /**
     * 最大连续空白字符数
     */
    private int maxNumChunks = 10000;
    /**
     * 是否保留 separator
     */
    private boolean keepSeparator = true;
    /**
     * 语义分段的最大字符数，超过后会在同一标题下继续拆分
     */
    private int semanticSectionMaxChars = 1200;
    /**
     * 预览接口默认返回的 chunk 数
     */
    private int previewChunkLimit = 8;
}
