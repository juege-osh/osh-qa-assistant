package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

@Data
public class KnowledgeLibRecallChunkVO {
    /**
     * 命中顺序
     */
    private Integer index;
    /**
     * 来源文件名
     */
    private String fileName;
    /**
     * 文档 id
     */
    private String documentId;
    /**
     * 召回得分
     */
    private Double score;
    /**
     * chunk 内容摘要
     */
    private String content;
}
