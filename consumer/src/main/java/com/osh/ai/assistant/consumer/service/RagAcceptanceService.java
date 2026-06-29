package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.consumer.bean.req.invokerecord.RagAcceptanceBatchSaveReq;
import com.osh.ai.assistant.consumer.bean.vo.RagAcceptanceBatchVO;

import java.util.List;

public interface RagAcceptanceService {
    Long saveBatch(RagAcceptanceBatchSaveReq req);

    List<RagAcceptanceBatchVO> listMine();

    RagAcceptanceBatchVO detail(Long id);
}
