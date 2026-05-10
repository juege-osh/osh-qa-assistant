package com.osh.ai.assistant.manager.service.impl;

import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.manager.bean.req.app.AppPageReq;
import com.osh.ai.assistant.manager.bean.vo.AppVO;
import com.osh.ai.assistant.manager.mapper.AppMapper;
import com.osh.ai.assistant.manager.service.AppService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
/**
 * 应用信息业务实现类

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppServiceImpl extends ServiceImpl<AppMapper,AppDO> implements AppService {
    @Override
    public Result<List<AppVO>> queryPage(AppPageReq pageReq) {
        Long count = getBaseMapper().queryCount(pageReq);
        if (count.equals(CommonConstants.ZERO_LONG)) {
            return Result.buildSuccess(Collections.emptyList(), count);
        }
        List<AppVO> vos = getBaseMapper().queryList(pageReq);
        return Result.buildSuccess(vos, count);
    }
}
