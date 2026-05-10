package com.osh.ai.assistant.manager.bean.req.manager;

import com.osh.ai.assistant.common.bean.req.BasePageReq;
import com.osh.ai.assistant.common.enums.SexEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员分页查询入参
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ManagerPageReq extends BasePageReq {
    /**
     * 用户名
     */
    private String username;
    /**
     * 姓名
     */
    private String realName;
    /**
     * @see SexEnum
     */
    private Integer sex;
}
