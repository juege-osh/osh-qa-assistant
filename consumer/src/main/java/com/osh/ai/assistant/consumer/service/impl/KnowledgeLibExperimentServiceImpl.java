package com.osh.ai.assistant.consumer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO;
import com.osh.ai.assistant.common.bean.entity.KnowledgeLibExperimentDO;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentPublishReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentRecommendReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentRenameReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentSaveReq;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibExperimentVO;
import com.osh.ai.assistant.consumer.elt.RagDocumentSplitService;
import com.osh.ai.assistant.consumer.elt.RagSplitRuntimeConfig;
import com.osh.ai.assistant.consumer.mapper.KnowledgeLibMapper;
import com.osh.ai.assistant.consumer.mapper.KnowledgeLibExperimentMapper;
import com.osh.ai.assistant.consumer.service.KnowledgeLibExperimentService;
import com.osh.ai.assistant.consumer.service.KnowledgeLibService;
import com.osh.ai.assistant.consumer.service.UploadFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeLibExperimentServiceImpl implements KnowledgeLibExperimentService {
    private final KnowledgeLibExperimentMapper knowledgeLibExperimentMapper;
    private final KnowledgeLibMapper knowledgeLibMapper;
    private final KnowledgeLibService knowledgeLibService;
    private final UploadFileService uploadFileService;
    private final RagDocumentSplitService ragDocumentSplitService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(KnowledgeLibExperimentSaveReq req) {
        knowledgeLibService.requireOwnedEntity(req.getLibId());
        KnowledgeLibExperimentDO entity = ConvertUtil.convert(req, KnowledgeLibExperimentDO.class);
        if (entity.getRecommended() == null) {
            entity.setRecommended(0);
        }
        if (entity.getRecommended() == 1) {
            clearRecommended(req.getLibId());
        }
        knowledgeLibExperimentMapper.insert(entity);
    }

    @Override
    public List<KnowledgeLibExperimentVO> listByLibId(Long libId) {
        KnowledgeLibDO lib = knowledgeLibService.requireOwnedEntity(libId);
        LambdaQueryWrapper<KnowledgeLibExperimentDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(KnowledgeLibExperimentDO::getLibId, libId)
            .orderByDesc(KnowledgeLibExperimentDO::getCreatedTime)
            .orderByDesc(KnowledgeLibExperimentDO::getId);
        List<KnowledgeLibExperimentVO> list = ConvertUtil.convert(knowledgeLibExperimentMapper.selectList(lqw), KnowledgeLibExperimentVO.class);
        for (KnowledgeLibExperimentVO item : list) {
            item.setActive(lib.getActiveExperimentId() != null && lib.getActiveExperimentId().equals(item.getId()) ? 1 : 0);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(KnowledgeLibExperimentRenameReq req) {
        KnowledgeLibExperimentDO entity = requireOwnedEntity(req.getId());
        entity.setVersionName(req.getVersionName());
        knowledgeLibExperimentMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRecommended(KnowledgeLibExperimentRecommendReq req) {
        knowledgeLibService.requireOwnedEntity(req.getLibId());
        KnowledgeLibExperimentDO entity = requireOwnedEntity(req.getId());
        if (!req.getLibId().equals(entity.getLibId())) {
            throw new BizEx("实验版本与知识库不匹配");
        }
        clearRecommended(req.getLibId());
        entity.setRecommended(1);
        entity.setRecommendReason(req.getRecommendReason());
        knowledgeLibExperimentMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(KnowledgeLibExperimentPublishReq req) {
        KnowledgeLibDO lib = knowledgeLibService.requireOwnedEntity(req.getLibId());
        KnowledgeLibExperimentDO entity = requireOwnedEntity(req.getId());
        if (!req.getLibId().equals(entity.getLibId())) {
            throw new BizEx("实验版本与知识库不匹配");
        }
        LambdaUpdateWrapper<KnowledgeLibDO> luw = new LambdaUpdateWrapper<>();
        luw.eq(KnowledgeLibDO::getId, lib.getId())
            .set(KnowledgeLibDO::getActiveSplitStrategy, entity.getSplitStrategy())
            .set(KnowledgeLibDO::getActiveSplitConfigJson, entity.getSplitConfigJson())
            .set(KnowledgeLibDO::getActiveExperimentId, entity.getId())
            .set(KnowledgeLibDO::getActiveExperimentName, entity.getVersionName());
        knowledgeLibMapper.update(new KnowledgeLibDO(), luw);
        RagSplitRuntimeConfig config = ragDocumentSplitService.buildConfigFromSnapshot(entity.getSplitStrategy(), entity.getSplitConfigJson());
        uploadFileService.rebuildByLibId(lib.getId(), config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        KnowledgeLibExperimentDO entity = requireOwnedEntity(id);
        knowledgeLibExperimentMapper.deleteById(entity.getId());
    }

    private KnowledgeLibExperimentDO requireOwnedEntity(Long id) {
        KnowledgeLibExperimentDO entity = knowledgeLibExperimentMapper.selectById(id);
        if (entity == null) {
            throw new BizEx("实验版本不存在");
        }
        knowledgeLibService.requireOwnedEntity(entity.getLibId());
        return entity;
    }

    private void clearRecommended(Long libId) {
        LambdaUpdateWrapper<KnowledgeLibExperimentDO> luw = new LambdaUpdateWrapper<>();
        luw.eq(KnowledgeLibExperimentDO::getLibId, libId)
            .set(KnowledgeLibExperimentDO::getRecommended, 0);
        knowledgeLibExperimentMapper.update(new KnowledgeLibExperimentDO(), luw);
    }
}
