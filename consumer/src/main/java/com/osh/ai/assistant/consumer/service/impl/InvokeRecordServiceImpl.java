package com.osh.ai.assistant.consumer.service.impl;

import com.osh.ai.assistant.common.bean.entity.InvokeRecordDO;
import com.osh.ai.assistant.common.bean.entity.InvokeRecordDetailDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.InvokeRecordPageReq;
import com.osh.ai.assistant.consumer.bean.vo.InvokeRecordOverviewVO;
import com.osh.ai.assistant.consumer.bean.vo.InvokeRecordVO;
import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;
import com.osh.ai.assistant.consumer.mapper.InvokeRecordMapper;
import com.osh.ai.assistant.consumer.service.InvokeRecordDetailService;
import com.osh.ai.assistant.consumer.service.InvokeRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 调用记录业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvokeRecordServiceImpl extends ServiceImpl<InvokeRecordMapper, InvokeRecordDO> implements InvokeRecordService {
    @Resource
    private InvokeRecordDetailService invokeRecordDetailService;
    @Override
    public Result<List<InvokeRecordVO>> queryPage(InvokeRecordPageReq pageReq) {
        pageReq.setUserId(UserContext.getUserId());
        Long count = getBaseMapper().queryCount(pageReq);
        if (count.equals(CommonConstants.ZERO_LONG)) {
            return Result.buildSuccess(Collections.emptyList(), count);
        }
        List<InvokeRecordVO> vos = getBaseMapper().queryList(pageReq);
        return Result.buildSuccess(vos, count);
    }

    @Override
    public Result<InvokeRecordOverviewVO> queryOverview() {
        InvokeRecordOverviewVO overview = getBaseMapper().queryOverview(UserContext.getUserId());
        if (overview == null) {
            overview = new InvokeRecordOverviewVO();
        }
        return Result.buildSuccess(overview);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvokeRecordDO record(InvokeRecordBuilder builder) {
        // 计算主记录的状态
        List<InvokeRecordDetailDO> detailList = invokeRecordDetailService.selectByInvokeRecordId(builder.getId());
        builder.setDetailList(detailList);
        InvokeRecordDO recordDO = builder.build();
        save(recordDO);
        return recordDO;
    }
}
