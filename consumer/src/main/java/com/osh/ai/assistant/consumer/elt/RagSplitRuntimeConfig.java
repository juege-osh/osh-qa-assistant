package com.osh.ai.assistant.consumer.elt;

import lombok.Data;

@Data
public class RagSplitRuntimeConfig {
    private String strategy;
    private int chunkSize;
    private int minChunkSizeChars;
    private int minChunkLengthToEmbed;
    private int maxNumChunks;
    private boolean keepSeparator;
    private int semanticSectionMaxChars;
    private int previewChunkLimit;
}
