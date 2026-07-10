package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

@Data
public class RagSplitChunkVO {
    /**
     * chunk 序号，从 1 开始
     */
    private Integer index;
    /**
     * chunk 字符数
     */
    private Integer length;
    /**
     * chunk 内容
     */
    private String content;
}
