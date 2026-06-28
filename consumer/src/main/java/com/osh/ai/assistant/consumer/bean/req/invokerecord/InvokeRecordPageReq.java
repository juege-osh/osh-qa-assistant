package com.osh.ai.assistant.consumer.bean.req.invokerecord;
import com.osh.ai.assistant.common.bean.req.BasePageReq;
import com.osh.ai.assistant.common.enums.InvokeStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 调用记录分页查询入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvokeRecordPageReq extends BasePageReq {
    /**
     * 用户id
     */
    @JsonIgnore
    private Long userId;
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
    /**
     * 应用名称关键词
     */
    private String appName;
    /**
     * 查询词关键词
     */
    private String userInputKeyword;
}
