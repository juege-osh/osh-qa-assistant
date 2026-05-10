package com.osh.ai.assistant.manager.bean.req.user;

import com.osh.ai.assistant.common.bean.req.BasePageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表分页查询入参
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageReq extends BasePageReq {
    /**
     * 用户名
     */
    private String username;
}
