package com.osh.ai.assistant.manager.service.impl;

import com.osh.ai.assistant.common.bean.entity.InvokeRecordDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.manager.bean.req.invokerecord.InvokeRecordPageReq;
import com.osh.ai.assistant.manager.bean.vo.InvokeRecordOverviewVO;
import com.osh.ai.assistant.manager.bean.vo.InvokeRecordVO;
import com.osh.ai.assistant.manager.mapper.InvokeRecordMapper;
import com.osh.ai.assistant.manager.service.InvokeRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 调用记录业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvokeRecordServiceImpl extends ServiceImpl<InvokeRecordMapper, InvokeRecordDO> implements InvokeRecordService {

    @Override
    public Result<List<InvokeRecordVO>> queryPage(InvokeRecordPageReq pageReq) {
        Long count = getBaseMapper().queryCount(pageReq);
        if (count.equals(CommonConstants.ZERO_LONG)) {
            return Result.buildSuccess(Collections.emptyList(), count);
        }
        List<InvokeRecordVO> vos = getBaseMapper().queryList(pageReq);
        return Result.buildSuccess(vos, count);
    }

    @Override
    public Result<InvokeRecordOverviewVO> queryOverview() {
        InvokeRecordOverviewVO overview = getBaseMapper().queryOverview();
        if (overview == null) {
            overview = new InvokeRecordOverviewVO();
        }
        return Result.buildSuccess(overview);
    }
}
