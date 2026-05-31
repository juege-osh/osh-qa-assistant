package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.osh.ai.assistant.common.bean.entity.UploadFileDO;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.enums.UploadFileStatusEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.common.util.PageUtil;
import com.osh.ai.assistant.consumer.bean.dto.StoreResultDTO;
import com.osh.ai.assistant.consumer.bean.req.uploadfile.UploadFileAddReq;
import com.osh.ai.assistant.consumer.bean.req.uploadfile.UploadFilePageReq;
import com.osh.ai.assistant.consumer.bean.req.uploadfile.UploadFileUpdateStatusReq;
import com.osh.ai.assistant.consumer.bean.vo.UploadFileVO;
import com.osh.ai.assistant.consumer.elt.Storage;
import com.osh.ai.assistant.consumer.mapper.KnowledgeLibMapper;
import com.osh.ai.assistant.consumer.mapper.UploadFileMapper;
import com.osh.ai.assistant.consumer.service.UploadFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文件信息业务实现类

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadFileServiceImpl extends ServiceImpl<UploadFileMapper, UploadFileDO> implements UploadFileService {
    @Resource
    private Storage storage;
    @Resource
    private KnowledgeLibMapper knowledgeLibMapper;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(UploadFileAddReq addReq) {
        UploadFileDO entity = ConvertUtil.convert(addReq,UploadFileDO.class);
        entity.setFileName(addReq.getOriginalFileName());
        entity.setStatus(UploadFileStatusEnum.ENABLED.getCode());
        // 存储到向量数据库中
        StoreResultDTO storeResultDTO = storage.store(addReq.getStorePath(), addReq.getLibId());
        entity.setCharCount(storeResultDTO.getCharCount());
        entity.setDocIds(JSONUtil.toJsonStr(storeResultDTO.getDocIds()));
        save(entity);
    }

    @Override
    public Result<List<UploadFileVO>> queryPage(UploadFilePageReq pageReq) {
        assertLibOwned(pageReq.getLibId());
        IPage<UploadFileDO> iPage = PageUtil.buildPage(pageReq);
        LambdaQueryWrapper<UploadFileDO> lqw = new LambdaQueryWrapper<>();
        // 拼接查询条件
        lqw.like(StrUtil.isNotBlank(pageReq.getFileName()),UploadFileDO::getFileName,pageReq.getFileName());
        lqw.eq(UploadFileDO::getLibId,pageReq.getLibId());
        lqw.orderByDesc(UploadFileDO::getId);
        IPage<UploadFileDO> page = page(iPage, lqw);
        return ConvertUtil.convert(page,UploadFileVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        UploadFileDO uploadFileDO = requireOwnedEntity(id);
        // 从向量数据库中删除
        storage.deleteByIds(uploadFileDO.getDocIds());
        removeById(id);
    }

    @Override
    public UploadFileVO queryById(Long id) {
        UploadFileDO entity = requireOwnedEntity(id);
        return ConvertUtil.convert(entity,UploadFileVO.class);
    }

    @Override
    public UploadFileDO requireOwnedEntity(Long id) {
        UploadFileDO entity = getById(id);
        if (entity == null) {
            throw new BizEx("文件不存在");
        }
        assertLibOwned(entity.getLibId());
        return entity;
    }

    @Override
    public List<UploadFileDO> selectByLibId(Long libId) {
        LambdaQueryWrapper<UploadFileDO> lqw = Wrappers.<UploadFileDO>lambdaQuery()
            .eq(UploadFileDO::getLibId, libId);
        return list(lqw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrRecallCount(List<String> docIds) {
        List<UploadFileDO> uploadFile2updateList = new ArrayList<>();
        // 如果多个docId对应一个文件,则只算召回一次
        Set<Long> uploadFileId2UpdateSet = new HashSet<>();
        for (String docId : docIds) {
            UploadFileDO uploadFileDO = getBaseMapper().selectByDocId(docId);
            if (uploadFileDO == null) {
                continue;
            }
            if (!uploadFileId2UpdateSet.contains(uploadFileDO.getId())) {
                uploadFileId2UpdateSet.add(uploadFileDO.getId());
                // 召回次数+1
                uploadFileDO.setRecallCount(uploadFileDO.getRecallCount() + 1);
                uploadFile2updateList.add(uploadFileDO);
            }
        }
        updateBatchById(uploadFile2updateList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(UploadFileUpdateStatusReq req) {
        UploadFileDO uploadFileDO = requireOwnedEntity(req.getId());
        if (uploadFileDO.getStatus().equals(req.getStatus())) {
            // 一致则无需更新
            return;
        }
        uploadFileDO.setStatus(req.getStatus());
        if (UploadFileStatusEnum.DISABLED.getCode().equals(req.getStatus())) {
            // 从向量db中删除
            storage.deleteByIds(uploadFileDO.getDocIds());
            uploadFileDO.setCharCount(0L);
            uploadFileDO.setDocIds(null);
        }else {
            // 重新生成后更新
            StoreResultDTO storeResultDTO = storage.store(uploadFileDO.getStorePath(), uploadFileDO.getLibId());
            uploadFileDO.setCharCount(storeResultDTO.getCharCount());
            uploadFileDO.setDocIds(JSONUtil.toJsonStr(storeResultDTO.getDocIds()));
        }
        // 更新字段为null的问题
        LambdaUpdateWrapper<UploadFileDO> luw = new LambdaUpdateWrapper<>();
        luw.set(UploadFileDO::getStatus,uploadFileDO.getStatus())
            .set(UploadFileDO::getCharCount,uploadFileDO.getCharCount())
            // 若avatarPath为null,用这种方式也能更新,即null不会被mp忽略
            .set(UploadFileDO::getDocIds,uploadFileDO.getDocIds())
            .eq(UploadFileDO::getId,uploadFileDO.getId());
        update(new UploadFileDO(),luw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByLibId(Long libId) {
        assertLibOwned(libId);
        LambdaQueryWrapper<UploadFileDO> lqw = Wrappers.<UploadFileDO>lambdaQuery()
            .eq(UploadFileDO::getLibId, libId);
        List<UploadFileDO> list = list(lqw);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        for (UploadFileDO uploadFileDO : list) {
            // 从向量db中清除关联的文档
            storage.deleteByIds(uploadFileDO.getDocIds());
        }
        remove(lqw);
    }

    private void assertLibOwned(Long libId) {
        LambdaQueryWrapper<com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO> queryWrapper = Wrappers.<com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO>lambdaQuery()
            .eq(com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO::getId, libId)
            .eq(com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO::getUserId, UserContext.getUserId());
        if (knowledgeLibMapper.selectCount(queryWrapper) == 0) {
            throw new BizEx("知识库不存在或无权限访问");
        }
    }
}
