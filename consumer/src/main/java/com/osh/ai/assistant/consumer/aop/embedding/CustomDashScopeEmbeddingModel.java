package com.osh.ai.assistant.consumer.aop.embedding;

import cn.hutool.core.util.ReflectUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.*;
import org.springframework.aop.framework.AopContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 因为要统计token需要拦截{@link EmbeddingModel#call(EmbeddingRequest)},但
 * 外部调用的{@link EmbeddingModel#embed}内是通过this.call的方式调用的,this不是
 * 代理对象,通过aop没办法拦截到调用,因此本类替换this为代理对象
 */
public class CustomDashScopeEmbeddingModel extends DashScopeEmbeddingModel {
    public CustomDashScopeEmbeddingModel(DashScopeApi dashScopeApi) {
        super(dashScopeApi);
    }

    public CustomDashScopeEmbeddingModel(DashScopeApi dashScopeApi, MetadataMode metadataMode) {
        super(dashScopeApi, metadataMode);
    }

    public CustomDashScopeEmbeddingModel(DashScopeApi dashScopeApi, MetadataMode metadataMode, DashScopeEmbeddingOptions dashScopeEmbeddingOptions) {
        super(dashScopeApi, metadataMode, dashScopeEmbeddingOptions);
    }

    public CustomDashScopeEmbeddingModel(DashScopeApi dashScopeApi, MetadataMode metadataMode, DashScopeEmbeddingOptions dashScopeEmbeddingOptions, RetryTemplate retryTemplate) {
        super(dashScopeApi, metadataMode, dashScopeEmbeddingOptions, retryTemplate);
    }

    public CustomDashScopeEmbeddingModel(DashScopeApi dashScopeApi, MetadataMode metadataMode, DashScopeEmbeddingOptions options, RetryTemplate retryTemplate, ObservationRegistry observationRegistry) {
        super(dashScopeApi, metadataMode, options, retryTemplate, observationRegistry);
    }

    /**
     * 仅替换原方法中的this.call为代理对象
     */
    @Override
    public List<float[]> embed(List<String> texts) {
        Assert.notNull(texts, "Texts must not be null");
        EmbeddingModel embeddingModel = (EmbeddingModel) AopContext.currentProxy();
        Object fieldValue = ReflectUtil.getFieldValue(this, "defaultOptions");
        // 修改为代理对象
        return embeddingModel.call(new EmbeddingRequest(texts, (EmbeddingOptions) fieldValue))
            .getResults()
            .stream()
            .map(Embedding::getOutput)
            .toList();
    }
    /**
     * 仅替换原方法中的this.call为代理对象
     */
    @Override
    public List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {
        Object fieldValue = ReflectUtil.getFieldValue(this, "defaultOptions");
        EmbeddingOptions defaultOptions = (EmbeddingOptions) fieldValue;

        EmbeddingModel embeddingModel = (EmbeddingModel) AopContext.currentProxy();

        if (options.getModel() == null && options.getDimensions() == null && defaultOptions != null) {
            options = defaultOptions;
        }

        Assert.notNull(documents, "Documents must not be null");
        List<float[]> embeddings = new ArrayList<>(documents.size());
        List<List<Document>> batch = batchingStrategy.batch(documents);
        for (List<Document> subBatch : batch) {
            List<String> texts = subBatch.stream().map(Document::getText).toList();
            EmbeddingRequest request = new EmbeddingRequest(texts, options);
            // 修改为代理对象
            EmbeddingResponse response = embeddingModel.call(request);
            for (int i = 0; i < subBatch.size(); i++) {
                embeddings.add(response.getResults().get(i).getOutput());
            }
        }
        Assert.isTrue(embeddings.size() == documents.size(),
            "Embeddings must have the same number as that of the documents");
        return embeddings;
    }
    /**
     * 仅替换原方法中的this.call为代理对象
     */
    @Override
    public EmbeddingResponse embedForResponse(List<String> texts) {
        Assert.notNull(texts, "Texts must not be null");
        EmbeddingModel embeddingModel = (EmbeddingModel) AopContext.currentProxy();
        Object fieldValue = ReflectUtil.getFieldValue(this, "defaultOptions");
        // 修改为代理对象
        return embeddingModel.call(new EmbeddingRequest(texts, (EmbeddingOptions) fieldValue));
    }
}
