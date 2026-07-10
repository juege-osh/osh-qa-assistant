package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.osh.ai.assistant.common.bean.entity.AppPublishConfigDO;
import com.osh.ai.assistant.common.enums.AppPublishAccessTypeEnum;
import com.osh.ai.assistant.common.enums.YesNoEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.consumer.bean.req.app.AppPublishSaveReq;
import com.osh.ai.assistant.consumer.bean.vo.AppPublishConfigVO;
import com.osh.ai.assistant.consumer.mapper.AppPublishConfigMapper;
import com.osh.ai.assistant.consumer.service.AppPublishConfigService;
import com.osh.ai.assistant.consumer.service.AppService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 应用公开发布配置业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppPublishConfigServiceImpl extends ServiceImpl<AppPublishConfigMapper, AppPublishConfigDO>
    implements AppPublishConfigService {

    @Resource
    private AppService appService;

    @Override
    public AppPublishConfigVO queryByAppId(Long appId) {
        appService.requireOwnedEntity(appId);
        AppPublishConfigDO config = getByAppId(appId);
        if (config == null) {
            AppPublishConfigVO vo = new AppPublishConfigVO();
            vo.setAppId(appId);
            vo.setEnabled(YesNoEnum.N.getCode());
            vo.setSlug(buildDefaultSlug(appId));
            vo.setAccessType(AppPublishAccessTypeEnum.PUBLIC.getCode());
            vo.setHasPassword(Boolean.FALSE);
            return vo;
        }
        return convert(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppPublishConfigVO saveConfig(AppPublishSaveReq req) {
        appService.requireOwnedEntity(req.getAppId());
        String slug = normalizeSlug(req.getSlug());
        AppPublishConfigDO existed = getByAppId(req.getAppId());
        ensureSlugAvailable(slug, existed == null ? null : existed.getId());
        if (AppPublishAccessTypeEnum.PASSWORD.getCode().equals(req.getAccessType())
            && StrUtil.isBlank(req.getAccessPassword())
            && (existed == null || StrUtil.isBlank(existed.getPasswordHash()))) {
            throw new BizEx("密码访问模式下请先设置访问密码");
        }

        if (existed == null) {
            AppPublishConfigDO entity = new AppPublishConfigDO();
            entity.setAppId(req.getAppId());
            entity.setEnabled(req.getEnabled());
            entity.setSlug(slug);
            entity.setAccessType(req.getAccessType());
            if (AppPublishAccessTypeEnum.PASSWORD.getCode().equals(req.getAccessType())) {
                entity.setPasswordHash(BCrypt.hashpw(req.getAccessPassword()));
            }
            save(entity);
            return convert(entity);
        }

        LambdaUpdateWrapper<AppPublishConfigDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
            .eq(AppPublishConfigDO::getId, existed.getId())
            .set(AppPublishConfigDO::getEnabled, req.getEnabled())
            .set(AppPublishConfigDO::getSlug, slug)
            .set(AppPublishConfigDO::getAccessType, req.getAccessType());
        if (AppPublishAccessTypeEnum.PUBLIC.getCode().equals(req.getAccessType())) {
            updateWrapper.set(AppPublishConfigDO::getPasswordHash, null);
        } else if (StrUtil.isNotBlank(req.getAccessPassword())) {
            updateWrapper.set(AppPublishConfigDO::getPasswordHash, BCrypt.hashpw(req.getAccessPassword()));
        }
        update(new AppPublishConfigDO(), updateWrapper);
        return convert(getById(existed.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long appId) {
        appService.requireOwnedEntity(appId);
        AppPublishConfigDO config = getByAppId(appId);
        if (config == null) {
            return;
        }
        LambdaUpdateWrapper<AppPublishConfigDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
            .eq(AppPublishConfigDO::getId, config.getId())
            .set(AppPublishConfigDO::getEnabled, YesNoEnum.N.getCode());
        update(new AppPublishConfigDO(), updateWrapper);
    }

    @Override
    public AppPublishConfigDO requireEnabledBySlug(String slug) {
        LambdaQueryWrapper<AppPublishConfigDO> queryWrapper = Wrappers.<AppPublishConfigDO>lambdaQuery()
            .eq(AppPublishConfigDO::getSlug, normalizeSlug(slug))
            .eq(AppPublishConfigDO::getEnabled, YesNoEnum.Y.getCode());
        AppPublishConfigDO config = getOne(queryWrapper, false);
        if (config == null) {
            throw new BizEx("公开应用不存在或已关闭");
        }
        return config;
    }

    private AppPublishConfigDO getByAppId(Long appId) {
        LambdaQueryWrapper<AppPublishConfigDO> queryWrapper = Wrappers.<AppPublishConfigDO>lambdaQuery()
            .eq(AppPublishConfigDO::getAppId, appId);
        return getOne(queryWrapper, false);
    }

    private void ensureSlugAvailable(String slug, Long existedId) {
        LambdaQueryWrapper<AppPublishConfigDO> queryWrapper = Wrappers.<AppPublishConfigDO>lambdaQuery()
            .eq(AppPublishConfigDO::getSlug, slug)
            .ne(existedId != null, AppPublishConfigDO::getId, existedId);
        if (count(queryWrapper) > 0) {
            throw new BizEx("公开链接标识已被使用");
        }
    }

    private AppPublishConfigVO convert(AppPublishConfigDO config) {
        AppPublishConfigVO vo = new AppPublishConfigVO();
        vo.setAppId(config.getAppId());
        vo.setEnabled(config.getEnabled());
        vo.setSlug(config.getSlug());
        vo.setAccessType(config.getAccessType());
        vo.setHasPassword(StrUtil.isNotBlank(config.getPasswordHash()));
        vo.setCustomDomain(config.getCustomDomain());
        vo.setCreatedTime(config.getCreatedTime());
        vo.setModifiedTime(config.getModifiedTime());
        return vo;
    }

    private String buildDefaultSlug(Long appId) {
        return "app-" + appId;
    }

    private String normalizeSlug(String slug) {
        if (StrUtil.isBlank(slug)) {
            return slug;
        }
        return slug.trim().toLowerCase();
    }
}
