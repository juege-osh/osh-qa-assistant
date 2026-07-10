package com.osh.ai.assistant.consumer.bean.vo;

import com.osh.ai.assistant.common.enums.AppPublishAccessTypeEnum;
import com.osh.ai.assistant.common.enums.YesNoEnum;
import com.osh.ai.assistant.common.util.EnumUtil;
import lombok.Data;

import java.util.Date;

/**
 * 应用公开发布配置视图对象
 */
@Data
public class AppPublishConfigVO {
    /**
     * 应用id
     */
    private Long appId;
    /**
     * 是否启用公开访问
     */
    private Integer enabled;
    /**
     * 是否启用公开访问描述
     */
    private String enabledDesc;
    /**
     * 公开访问标识
     */
    private String slug;
    /**
     * 访问方式
     */
    private String accessType;
    /**
     * 访问方式描述
     */
    private String accessTypeDesc;
    /**
     * 是否已经设置访问密码
     */
    private Boolean hasPassword;
    /**
     * 自定义访问域名
     */
    private String customDomain;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 修改时间
     */
    private Date modifiedTime;

    public String getEnabledDesc() {
        return EnumUtil.getDescByCode(enabled, YesNoEnum.class);
    }

    public String getAccessTypeDesc() {
        return EnumUtil.getDescByCode(accessType, AppPublishAccessTypeEnum.class);
    }
}
