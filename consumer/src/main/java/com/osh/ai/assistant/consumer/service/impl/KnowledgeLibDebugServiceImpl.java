package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.osh.ai.assistant.common.bean.entity.UploadFileDO;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibRecallDebugReq;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibRecallChunkVO;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibRecallDebugVO;
import com.osh.ai.assistant.consumer.config.properties.RagSplitProperties;
import com.osh.ai.assistant.consumer.service.KnowledgeLibDebugService;
import com.osh.ai.assistant.consumer.service.KnowledgeLibService;
import com.osh.ai.assistant.consumer.service.UploadFileService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeLibDebugServiceImpl implements KnowledgeLibDebugService {
    private static final int PREVIEW_LENGTH = 240;

    @Resource
    private KnowledgeLibService knowledgeLibService;
    @Resource
    private UploadFileService uploadFileService;
    @Resource
    private VectorStore vectorStore;
    @Resource
    private AiChatServiceImpl aiChatService;
    @Resource
    private RagSplitProperties ragSplitProperties;

    @Override
    public KnowledgeLibRecallDebugVO debugRecall(KnowledgeLibRecallDebugReq req) {
        knowledgeLibService.requireOwnedEntity(req.getLibId());
        SearchRequest searchRequest = SearchRequest.builder()
            .query(req.getQuery())
            .topK(req.getTopK())
            .filterExpression(CommonConstants.LIB_ID_STR_KEY + " == '" + req.getLibId() + "'")
            .build();
        List<Document> rawDocuments = vectorStore.similaritySearch(searchRequest);
        List<Document> rerankDocuments = aiChatService.doRerank(req.getQuery(), rawDocuments);

        KnowledgeLibRecallDebugVO ret = new KnowledgeLibRecallDebugVO();
        ret.setQuery(req.getQuery());
        ret.setTopK(req.getTopK());
        ret.setRawHitCount(rawDocuments == null ? 0 : rawDocuments.size());
        ret.setRerankHitCount(rerankDocuments == null ? 0 : rerankDocuments.size());
        ret.setSplitStrategy(ragSplitProperties.getStrategy());
        ret.setRawHits(buildChunkVos(rawDocuments));
        ret.setRerankHits(buildChunkVos(rerankDocuments));
        return ret;
    }

    private List<KnowledgeLibRecallChunkVO> buildChunkVos(List<Document> documents) {
        if (CollUtil.isEmpty(documents)) {
            return List.of();
        }
        Map<String, UploadFileDO> fileMap = buildFileMap(documents);
        List<KnowledgeLibRecallChunkVO> ret = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            KnowledgeLibRecallChunkVO vo = new KnowledgeLibRecallChunkVO();
            vo.setIndex(i + 1);
            vo.setDocumentId(document.getId());
            vo.setScore(document.getScore());
            UploadFileDO uploadFileDO = fileMap.get(document.getId());
            vo.setFileName(uploadFileDO == null ? "未找到来源文件" : uploadFileDO.getFileName());
            vo.setContent(truncate(document.getText()));
            ret.add(vo);
        }
        return ret;
    }

    private Map<String, UploadFileDO> buildFileMap(List<Document> documents) {
        List<String> docIds = documents.stream().map(Document::getId).toList();
        List<UploadFileDO> uploadFiles = uploadFileService.selectByDocIds(docIds);
        Map<String, UploadFileDO> ret = new LinkedHashMap<>();
        for (String docId : docIds) {
            for (UploadFileDO uploadFileDO : uploadFiles) {
                if (StrUtil.contains(uploadFileDO.getDocIds(), docId)) {
                    ret.putIfAbsent(docId, uploadFileDO);
                    break;
                }
            }
        }
        return ret;
    }

    private String truncate(String content) {
        if (StrUtil.isBlank(content)) {
            return "暂无内容";
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.length() <= PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_LENGTH) + "...";
    }
}
