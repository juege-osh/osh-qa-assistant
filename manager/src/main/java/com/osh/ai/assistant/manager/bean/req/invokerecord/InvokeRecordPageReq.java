package com.osh.ai.assistant.manager.bean.req.invokerecord;

import com.osh.ai.assistant.common.bean.req.BasePageReq;
import com.osh.ai.assistant.common.enums.InvokeStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 调用记录分页查询入参
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvokeRecordPageReq extends BasePageReq {
    /**
     * 用户名
     */
    private String username;
    /**
     * 状态
     *
     * @see InvokeStatusEnum
     */
    private Integer status;
    /**
     * 开始时间
     */
    private String startTime;
    /**
     * 结束时间
     */
    private String endTime;
}
