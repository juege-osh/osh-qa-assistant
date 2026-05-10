package com.osh.ai.assistant.manager.bean.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调用记录统计
 */
@Data
public class InvokeRecordStatisticVO {
    /**
     * 总流水
     */
    private BigDecimal totalFlow;
    /**
     * 供应商费用
     */
    private BigDecimal supplierAmount;
    /**
     * 收益
     */
    private BigDecimal profit;
}
