package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

/**
 * 调用记录总览
 */
@Data
public class InvokeRecordOverviewVO {
    /**
     * 调用总数
     */
    private Long totalCount;
    /**
     * 成功数
     */
    private Long successCount;
    /**
     * 失败数
     */
    private Long failCount;
    /**
     * token消耗总数
     */
    private Long totalCostToken;
    /**
     * 平均耗时
     */
    private Long avgCostTime;
}
