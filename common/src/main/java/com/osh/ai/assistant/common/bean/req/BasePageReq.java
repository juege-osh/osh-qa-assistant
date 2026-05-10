package com.osh.ai.assistant.common.bean.req;

import com.osh.ai.assistant.common.constants.CommonConstants;
import lombok.Data;


@Data
public class BasePageReq {

    private Long pageNow = CommonConstants.DEFAULT_PAGE_NOW;
    /**
     * 每页有多少条
     */
    private Long pageSize = CommonConstants.DEFAULT_PAGE_SIZE;

    public Long getStart() {
        return  (pageNow - 1) * pageSize;
    }
}
