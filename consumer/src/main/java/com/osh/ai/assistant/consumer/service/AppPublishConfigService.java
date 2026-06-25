package com.osh.ai.assistant.consumer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.osh.ai.assistant.common.bean.entity.AppPublishConfigDO;
import com.osh.ai.assistant.consumer.bean.req.app.AppPublishSaveReq;
import com.osh.ai.assistant.consumer.bean.vo.AppPublishConfigVO;

/**
 * 应用公开发布配置业务类
 */
public interface AppPublishConfigService extends IService<AppPublishConfigDO> {
    /**
     * 查询当前用户拥有的应用发布配置
     */
    AppPublishConfigVO queryByAppId(Long appId);

    /**
     * 保存当前用户拥有的应用发布配置
     */
    AppPublishConfigVO saveConfig(AppPublishSaveReq req);

    /**
     * 关闭当前用户拥有的应用公开访问
     */
    void disable(Long appId);

    /**
     * 查询已启用的公开配置
     */
    AppPublishConfigDO requireEnabledBySlug(String slug);
}
