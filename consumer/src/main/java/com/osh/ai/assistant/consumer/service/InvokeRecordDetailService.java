package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.common.bean.entity.InvokeRecordDetailDO;
import com.osh.ai.assistant.consumer.builder.InvokeRecordDetailBuilder;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
  * 调用记录明细业务类
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
public interface InvokeRecordDetailService extends IService<InvokeRecordDetailDO> {
    /**
    * 新增
    */
    void add(InvokeRecordDetailBuilder builder);

    List<InvokeRecordDetailDO> selectByInvokeRecordId(Long invokeRecordId);
}
