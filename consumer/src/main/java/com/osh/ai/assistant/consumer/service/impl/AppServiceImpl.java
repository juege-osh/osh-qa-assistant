package com.osh.ai.assistant.consumer.service.impl;
import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.common.enums.YesNoEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.consumer.bean.req.app.AppAddReq;
import com.osh.ai.assistant.consumer.bean.req.app.AppUpdateReq;
import com.osh.ai.assistant.consumer.bean.req.app.BindLibReq;
import com.osh.ai.assistant.consumer.bean.vo.AppVO;
import com.osh.ai.assistant.consumer.mapper.AppMapper;
import com.osh.ai.assistant.consumer.service.AppService;
import com.osh.ai.assistant.consumer.service.KnowledgeLibService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.osh.ai.assistant.consumer.bean.req.app.AppPageReq;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 应用信息业务实现类

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppServiceImpl extends ServiceImpl<AppMapper, AppDO> implements AppService {

    @Resource
    private KnowledgeLibService knowledgeLibService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(AppAddReq addReq) {
        AppDO entity = ConvertUtil.convert(addReq,AppDO.class);
        entity.setUserId(UserContext.getUserId());
        save(entity);
    }

    @Override
    public Result<List<AppVO>> queryPage(AppPageReq pageReq) {
        pageReq.setUserId(UserContext.getUserId());
        Long count = getBaseMapper().queryCount(pageReq);
        if (count.equals(CommonConstants.ZERO_LONG)) {
            return Result.buildSuccess(Collections.emptyList(), count);
        }
        List<AppVO> vos = getBaseMapper().queryList(pageReq);
        return Result.buildSuccess(vos, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        requireOwnedEntity(id);
        removeById(id);
    }

    @Override
    public AppVO queryById(Long id) {
        AppDO entity = requireOwnedEntity(id);
        AppVO vo = ConvertUtil.convert(entity, AppVO.class);
        KnowledgeLibDO related = entity.getLibId() == null ? null : knowledgeLibService.requireOwnedEntity(entity.getLibId());
        if (related != null) {
            vo.setLibId(related.getId());
            vo.setLibName(related.getLibName());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyById(AppUpdateReq updateReq) {
        requireOwnedEntity(updateReq.getId());
        LambdaUpdateWrapper<AppDO> luw = new LambdaUpdateWrapper<>();
        luw.set(AppDO::getAppName,updateReq.getAppName())
            .set(AppDO::getAppDesc,updateReq.getAppDesc())
            // 可以修改为null
            .set(AppDO::getIconPath,updateReq.getIconPath())
            .set(AppDO::getOutLibEnable,updateReq.getOutLibEnable())
            .eq(AppDO::getId,updateReq.getId());
        update(new AppDO(),luw);
    }

    @Override
    public void checkChatCondition(Long id) {
        AppDO app = requireOwnedEntity(id);
        if (app.getLibId() == null && !YesNoEnum.Y.getCode().equals(app.getOutLibEnable())) {
            throw new BizEx("请先关联知识库");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unBindLib(Long id) {
        requireOwnedEntity(id);
        LambdaUpdateWrapper<AppDO> luw = new LambdaUpdateWrapper<>();
        luw.set(AppDO::getLibId,null)
            .eq(AppDO::getId,id);
        update(new AppDO(),luw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindLib(BindLibReq req) {
        requireOwnedEntity(req.getId());
        knowledgeLibService.requireOwnedEntity(req.getLibId());
        LambdaUpdateWrapper<AppDO> luw = new LambdaUpdateWrapper<>();
        luw.set(AppDO::getLibId,req.getLibId())
            .eq(AppDO::getId,req.getId());
        update(new AppDO(),luw);
    }

    @Override
    public AppDO requireOwnedEntity(Long id) {
        LambdaQueryWrapper<AppDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppDO::getId, id)
            .eq(AppDO::getUserId, UserContext.getUserId());
        AppDO entity = getOne(queryWrapper, false);
        if (entity == null) {
            throw new BizEx("应用不存在或无权限访问");
        }
        return entity;
    }

    @Override
    public boolean hasBound(Long libId) {
        LambdaQueryWrapper<AppDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AppDO::getLibId,libId);
        return count(lqw) > 0;
    }
}
