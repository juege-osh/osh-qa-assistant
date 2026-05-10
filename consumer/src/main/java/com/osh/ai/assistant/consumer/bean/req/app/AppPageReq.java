package com.osh.ai.assistant.consumer.bean.req.app;

import com.osh.ai.assistant.common.bean.req.BasePageReq;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用信息分页查询入参
 *

 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppPageReq extends BasePageReq {
    @JsonIgnore
    private Long userId;
    /**
     * 应用名称
     */
    private String appName;
}
