package com.osh.ai.assistant.manager.mapper;
import com.osh.ai.assistant.common.bean.entity.InvokeRecordDO;
import com.osh.ai.assistant.manager.bean.req.invokerecord.InvokeRecordPageReq;
import com.osh.ai.assistant.manager.bean.vo.InvokeRecordOverviewVO;
import com.osh.ai.assistant.manager.bean.vo.InvokeRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 调用记录mapper类
 */
public interface InvokeRecordMapper extends BaseMapper<InvokeRecordDO> {

    Long queryCount(InvokeRecordPageReq pageReq);

    List<InvokeRecordVO> queryList(InvokeRecordPageReq pageReq);

    InvokeRecordOverviewVO queryOverview();
}
