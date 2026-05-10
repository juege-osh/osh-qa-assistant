package com.osh.ai.assistant.consumer.elt;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.consumer.bean.dto.StoreResultDTO;
import com.osh.ai.assistant.consumer.elt.reader.DocReader;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量存储实现
 */
@Component
public class VectorStorage implements Storage{
    @Resource
    private DocReader docReader;
    @Resource
    private VectorStore vectorStore;
    @Override
    public StoreResultDTO store(String storePath, Long libId) {
        List<Document> documents = docReader.read(storePath);
        // 对每一个文档(Document)分块,每chunk的size默认为800
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        documents = tokenTextSplitter.split(documents);
        List<String> docIds = new ArrayList<>();
        long charCount = 0L;
        // 为每个文档添加元数据
        for (Document document : documents) {
            // 这里最好用字符串类型的,底层是使用int存储的,long类型的可能导致精度丢失
            document.getMetadata().put(CommonConstants.LIB_ID_STR_KEY, String.valueOf(libId));
            docIds.add(document.getId());
            // 获取当前文档的内容
            String text = document.getText();
            if (StrUtil.isBlank(text)) {
                continue;
            }
            charCount += text.length();
        }
        // 把分割好的文档列表放入到向量数据库中
        vectorStore.add(documents);
        StoreResultDTO ret = new StoreResultDTO();
        ret.setCharCount(charCount);
        ret.setDocIds(docIds);
        return ret;
    }

    @Override
    public void deleteByIds(String docIds) {
        if (StrUtil.isBlank(docIds)) {
            return;
        }
        List<String> list = JSONUtil.toList(docIds, String.class);
        vectorStore.delete(list);
    }
}
