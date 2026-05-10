package com.osh.ai.assistant.consumer.service.impl;
import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.consumer.service.AppService;
import com.osh.ai.assistant.consumer.service.UploadFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibPageReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibAddReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibUpdateReq;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibVO;
import com.osh.ai.assistant.consumer.mapper.KnowledgeLibMapper;
import com.osh.ai.assistant.consumer.service.KnowledgeLibService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 知识库业务实现类

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeLibServiceImpl extends ServiceImpl<KnowledgeLibMapper, KnowledgeLibDO> implements KnowledgeLibService {
    @Resource
    private UploadFileService uploadFileService;
    @Resource
    private AppService appService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(KnowledgeLibAddReq addReq) {
        KnowledgeLibDO entity = ConvertUtil.convert(addReq,KnowledgeLibDO.class);
        entity.setUserId(UserContext.getUserId());
        save(entity);
    }

    @Override
    public Result<List<KnowledgeLibVO>> queryPage(KnowledgeLibPageReq pageReq) {
        pageReq.setUserId(UserContext.getUserId());
        Long count = getBaseMapper().queryCount(pageReq);
        if (count.equals(CommonConstants.ZERO_LONG)) {
            return Result.buildSuccess(Collections.emptyList(), count);
        }
        List<KnowledgeLibVO> vos = getBaseMapper().queryList(pageReq);
        return Result.buildSuccess(vos, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        boolean hasBound = appService.hasBound(id);
        if (hasBound) {
            throw new BizEx("请先解除当前知识库的绑定");
        }
        KnowledgeLibDO knowledgeLib = getById(id);
        if (knowledgeLib == null) {
            return;
        }
        uploadFileService.deleteByLibId(knowledgeLib.getId());
        removeById(id);
    }

    @Override
    public KnowledgeLibVO queryById(Long id) {
        KnowledgeLibDO entity = getById(id);
        return ConvertUtil.convert(entity,KnowledgeLibVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyById(KnowledgeLibUpdateReq updateReq) {
        KnowledgeLibDO entity = ConvertUtil.convert(updateReq,KnowledgeLibDO.class);
        updateById(entity);
    }

    @Override
    public List<KnowledgeLibVO> listAvailableLib(Long appId) {
        List<KnowledgeLibDO> knowledgeLibs = getBaseMapper().listUnUsed(UserContext.getUserId());
        if (appId != null) {
            // 加上自己的
            AppDO app = appService.getById(appId);
            // 如果已解绑则为空
            if (app != null && app.getLibId() != null) {
                knowledgeLibs.add(getById(app.getLibId()));
            }
        }
        return ConvertUtil.convert(knowledgeLibs,KnowledgeLibVO.class);
    }
}
