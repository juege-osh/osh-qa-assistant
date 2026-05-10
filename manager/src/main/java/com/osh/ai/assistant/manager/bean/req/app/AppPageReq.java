package com.osh.ai.assistant.manager.bean.req.app;

import com.osh.ai.assistant.common.bean.req.BasePageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用信息分页查询入参

 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppPageReq extends BasePageReq {
      /**
      * 应用名称
      */
     private String appName;
}
