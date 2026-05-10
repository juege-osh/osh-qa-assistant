package com.osh.ai.assistant.manager.service.impl;

import com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.manager.bean.req.knowledgelib.KnowledgeLibPageReq;
import com.osh.ai.assistant.manager.bean.vo.KnowledgeLibVO;
import com.osh.ai.assistant.manager.mapper.KnowledgeLibMapper;
import com.osh.ai.assistant.manager.service.KnowledgeLibService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
/**
 * 知识库业务实现类

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeLibServiceImpl extends ServiceImpl<KnowledgeLibMapper,KnowledgeLibDO> implements KnowledgeLibService {

    @Override
    public Result<List<KnowledgeLibVO>> queryPage(KnowledgeLibPageReq pageReq) {
        Long count = getBaseMapper().queryCount(pageReq);
        if (count.equals(CommonConstants.ZERO_LONG)) {
            return Result.buildSuccess(Collections.emptyList(), count);
        }
        List<KnowledgeLibVO> vos = getBaseMapper().queryList(pageReq);
        return Result.buildSuccess(vos, count);
    }
}
