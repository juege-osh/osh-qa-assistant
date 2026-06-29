package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeLibRecallDebugVO {
    /**
     * 当前问题
     */
    private String query;
    /**
     * 使用的 topK
     */
    private Integer topK;
    /**
     * 原始召回数
     */
    private Integer rawHitCount;
    /**
     * 重排后召回数
     */
    private Integer rerankHitCount;
    /**
     * 当前切分策略
     */
    private String splitStrategy;
    /**
     * 原始召回结果
     */
    private List<KnowledgeLibRecallChunkVO> rawHits;
    /**
     * 重排后结果
     */
    private List<KnowledgeLibRecallChunkVO> rerankHits;
}
