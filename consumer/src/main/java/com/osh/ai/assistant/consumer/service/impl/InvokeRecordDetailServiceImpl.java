package com.osh.ai.assistant.consumer.service.impl;

import com.osh.ai.assistant.common.bean.entity.InvokeRecordDetailDO;
import com.osh.ai.assistant.consumer.builder.InvokeRecordDetailBuilder;
import com.osh.ai.assistant.consumer.mapper.InvokeRecordDetailMapper;
import com.osh.ai.assistant.consumer.service.InvokeRecordDetailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 调用记录明细业务实现类

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvokeRecordDetailServiceImpl extends ServiceImpl<InvokeRecordDetailMapper, InvokeRecordDetailDO> implements InvokeRecordDetailService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(InvokeRecordDetailBuilder builder) {
        InvokeRecordDetailDO entity = builder.build();
        save(entity);
    }

    @Override
    public List<InvokeRecordDetailDO> selectByInvokeRecordId(Long invokeRecordId) {
        LambdaQueryWrapper<InvokeRecordDetailDO> lqw = Wrappers.<InvokeRecordDetailDO>lambdaQuery()
            .eq(InvokeRecordDetailDO::getInvokeRecordId, invokeRecordId);
        return list(lqw);
    }
}
