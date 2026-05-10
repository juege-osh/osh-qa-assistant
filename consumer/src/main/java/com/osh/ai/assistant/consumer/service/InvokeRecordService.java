package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.common.bean.entity.InvokeRecordDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.InvokeRecordPageReq;
import com.osh.ai.assistant.consumer.bean.vo.InvokeRecordOverviewVO;
import com.osh.ai.assistant.consumer.bean.vo.InvokeRecordVO;
import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
  * 调用记录业务类
  */
public interface InvokeRecordService extends IService<InvokeRecordDO> {
    /**
    * 分页查询
    */
    Result<List<InvokeRecordVO>> queryPage(InvokeRecordPageReq pageReq);

    /**
     * 查询总览
     */
    Result<InvokeRecordOverviewVO> queryOverview();

    InvokeRecordDO record(InvokeRecordBuilder builder);

}
