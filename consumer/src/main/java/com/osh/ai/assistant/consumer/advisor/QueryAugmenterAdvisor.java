package com.osh.ai.assistant.consumer.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;

import java.util.List;

/**
 * 查询增强
 */
public class QueryAugmenterAdvisor implements BaseAdvisor {
    private List<Document> documents;
    public QueryAugmenterAdvisor(List<Document> documents) {
        this.documents = documents;
    }

    /**
     * 会话执行之前被调用
     * 功能:使用{@link #documents}增强上下文
     * 提示词类型:{@link org.springframework.ai.chat.messages.MessageType}
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        // 原始的
        Query originalQuery = Query.builder()
            .text(chatClientRequest.prompt().getUserMessage().getText())
            .history(chatClientRequest.prompt().getInstructions())
            .build();
        // 查询增强对象
        QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder().build();
        // 增强后的对象
        Query augmentedQuery = queryAugmenter.augment(originalQuery, documents);
        return chatClientRequest
            // 创建一个新的对象
            .mutate()
            .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedQuery.text()))
            .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
