package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibRecallDebugReq;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibRecallDebugVO;

public interface KnowledgeLibDebugService {
    KnowledgeLibRecallDebugVO debugRecall(KnowledgeLibRecallDebugReq req);
}
