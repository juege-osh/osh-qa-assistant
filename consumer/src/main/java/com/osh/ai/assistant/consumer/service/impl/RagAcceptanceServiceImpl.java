package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.osh.ai.assistant.common.bean.entity.RagAcceptanceBatchDO;
import com.osh.ai.assistant.common.bean.entity.RagAcceptanceItemDO;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.RagAcceptanceBatchSaveReq;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.RagAcceptanceItemSaveReq;
import com.osh.ai.assistant.consumer.bean.vo.RagAcceptanceBatchVO;
import com.osh.ai.assistant.consumer.bean.vo.RagAcceptanceItemVO;
import com.osh.ai.assistant.consumer.mapper.RagAcceptanceBatchMapper;
import com.osh.ai.assistant.consumer.mapper.RagAcceptanceItemMapper;
import com.osh.ai.assistant.consumer.service.RagAcceptanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RagAcceptanceServiceImpl implements RagAcceptanceService {
    private final RagAcceptanceBatchMapper batchMapper;
    private final RagAcceptanceItemMapper itemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveBatch(RagAcceptanceBatchSaveReq req) {
        RagAcceptanceBatchDO entity;
        if (req.getId() == null) {
            entity = new RagAcceptanceBatchDO();
            entity.setUserId(UserContext.getUserId());
            fillBatchFields(entity, req);
            batchMapper.insert(entity);
        } else {
            entity = requireOwnedBatch(req.getId());
            fillBatchFields(entity, req);
            batchMapper.updateById(entity);
            LambdaUpdateWrapper<RagAcceptanceItemDO> deleteWrapper = new LambdaUpdateWrapper<>();
            deleteWrapper.eq(RagAcceptanceItemDO::getBatchId, entity.getId());
            itemMapper.delete(deleteWrapper);
        }

        for (RagAcceptanceItemSaveReq itemReq : req.getItems()) {
            RagAcceptanceItemDO item = ConvertUtil.convert(itemReq, RagAcceptanceItemDO.class);
            item.setId(null);
            item.setBatchId(entity.getId());
            itemMapper.insert(item);
        }
        return entity.getId();
    }

    @Override
    public List<RagAcceptanceBatchVO> listMine() {
        LambdaQueryWrapper<RagAcceptanceBatchDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(RagAcceptanceBatchDO::getUserId, UserContext.getUserId())
            .orderByDesc(RagAcceptanceBatchDO::getModifiedTime)
            .orderByDesc(RagAcceptanceBatchDO::getId);
        List<RagAcceptanceBatchDO> batches = batchMapper.selectList(lqw);
        List<RagAcceptanceBatchVO> result = new ArrayList<>();
        for (RagAcceptanceBatchDO batch : batches) {
            RagAcceptanceBatchVO vo = ConvertUtil.convert(batch, RagAcceptanceBatchVO.class);
            List<RagAcceptanceItemDO> items = listItems(batch.getId());
            vo.setItems(ConvertUtil.convert(items, RagAcceptanceItemVO.class));
            vo.setItemCount(items.size());
            vo.setPassCount((int) items.stream().filter(this::isPassItem).count());
            vo.setFollowUpCount((int) items.stream().filter(this::isFollowUpItem).count());
            result.add(vo);
        }
        return result;
    }

    @Override
    public RagAcceptanceBatchVO detail(Long id) {
        RagAcceptanceBatchDO batch = requireOwnedBatch(id);
        RagAcceptanceBatchVO vo = ConvertUtil.convert(batch, RagAcceptanceBatchVO.class);
        List<RagAcceptanceItemDO> items = listItems(batch.getId());
        vo.setItems(ConvertUtil.convert(items, RagAcceptanceItemVO.class));
        vo.setItemCount(items.size());
        vo.setPassCount((int) items.stream().filter(this::isPassItem).count());
        vo.setFollowUpCount((int) items.stream().filter(this::isFollowUpItem).count());
        return vo;
    }

    private void fillBatchFields(RagAcceptanceBatchDO entity, RagAcceptanceBatchSaveReq req) {
        entity.setBatchName(StrUtil.trim(req.getBatchName()));
        entity.setAppName(StrUtil.emptyToNull(StrUtil.trim(req.getAppName())));
        entity.setSceneType(StrUtil.emptyToNull(StrUtil.trim(req.getSceneType())));
        entity.setKnowledgeScope(StrUtil.emptyToNull(StrUtil.trim(req.getKnowledgeScope())));
        entity.setReleaseVersion(StrUtil.emptyToNull(StrUtil.trim(req.getReleaseVersion())));
        entity.setExperimentVersion(StrUtil.emptyToNull(StrUtil.trim(req.getExperimentVersion())));
        entity.setVersionRemark(StrUtil.emptyToNull(StrUtil.trim(req.getVersionRemark())));
        entity.setQuickView(StrUtil.emptyToNull(StrUtil.trim(req.getQuickView())));
        entity.setQuickViewDesc(StrUtil.emptyToNull(StrUtil.trim(req.getQuickViewDesc())));
        entity.setTesterName(StrUtil.emptyToNull(StrUtil.trim(req.getTesterName())));
        entity.setSummaryConclusion(StrUtil.emptyToNull(StrUtil.trim(req.getSummaryConclusion())));
        entity.setNextAction(StrUtil.emptyToNull(StrUtil.trim(req.getNextAction())));
        entity.setTestDate(parseDate(req.getTestDate()));
    }

    private Date parseDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return DateUtil.parse(value).toJdkDate();
    }

    private RagAcceptanceBatchDO requireOwnedBatch(Long id) {
        RagAcceptanceBatchDO batch = batchMapper.selectById(id);
        if (batch == null || !Objects.equals(batch.getUserId(), UserContext.getUserId())) {
            throw new BizEx("验收批次不存在或无权限访问");
        }
        return batch;
    }

    private List<RagAcceptanceItemDO> listItems(Long batchId) {
        LambdaQueryWrapper<RagAcceptanceItemDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(RagAcceptanceItemDO::getBatchId, batchId)
            .orderByAsc(RagAcceptanceItemDO::getId);
        return itemMapper.selectList(lqw);
    }

    private boolean isPassItem(RagAcceptanceItemDO item) {
        return isPass(item.getHitConclusion())
            && isPass(item.getGroundedConclusion())
            && isPass(item.getReadableConclusion())
            && isPass(item.getGracefulFailureConclusion());
    }

    private boolean isFollowUpItem(RagAcceptanceItemDO item) {
        return StrUtil.isNotBlank(item.getFollowUpCategory()) || StrUtil.isNotBlank(item.getFollowUpAction());
    }

    private boolean isPass(String value) {
        return "通过".equals(StrUtil.trim(value));
    }
}
