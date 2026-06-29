package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentRecommendReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentRenameReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentPublishReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentSaveReq;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibExperimentVO;

import java.util.List;

public interface KnowledgeLibExperimentService {
    void save(KnowledgeLibExperimentSaveReq req);

    List<KnowledgeLibExperimentVO> listByLibId(Long libId);

    void rename(KnowledgeLibExperimentRenameReq req);

    void markRecommended(KnowledgeLibExperimentRecommendReq req);

    void publish(KnowledgeLibExperimentPublishReq req);

    void deleteById(Long id);
}
