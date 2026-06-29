package com.osh.ai.assistant.consumer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.osh.ai.assistant.common.bean.entity.KnowledgeLibExperimentDO;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentRecommendReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentRenameReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentSaveReq;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibExperimentVO;
import com.osh.ai.assistant.consumer.mapper.KnowledgeLibExperimentMapper;
import com.osh.ai.assistant.consumer.service.KnowledgeLibExperimentService;
import com.osh.ai.assistant.consumer.service.KnowledgeLibService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeLibExperimentServiceImpl implements KnowledgeLibExperimentService {
    private final KnowledgeLibExperimentMapper knowledgeLibExperimentMapper;
    private final KnowledgeLibService knowledgeLibService;

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
        knowledgeLibService.requireOwnedEntity(libId);
        LambdaQueryWrapper<KnowledgeLibExperimentDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(KnowledgeLibExperimentDO::getLibId, libId)
            .orderByDesc(KnowledgeLibExperimentDO::getCreatedTime)
            .orderByDesc(KnowledgeLibExperimentDO::getId);
        return ConvertUtil.convert(knowledgeLibExperimentMapper.selectList(lqw), KnowledgeLibExperimentVO.class);
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
        knowledgeLibExperimentMapper.updateById(entity);
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
