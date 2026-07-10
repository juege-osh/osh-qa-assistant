package com.osh.ai.assistant.consumer.bean.vo;

import com.osh.ai.assistant.common.jackson.AddressableUrl;
import lombok.Data;

/**
 * 公开应用详情
 */
@Data
public class PublicAppDetailVO {
    /**
     * 公开访问标识
     */
    private String slug;
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 应用描述
     */
    private String appDesc;
    /**
     * 应用图标
     */
    @AddressableUrl
    private String iconPath;
    /**
     * 访问方式
     */
    private String accessType;
    /**
     * 是否需要访问密码
     */
    private Boolean passwordRequired;
}
