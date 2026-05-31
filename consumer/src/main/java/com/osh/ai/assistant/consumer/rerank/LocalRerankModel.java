package com.osh.ai.assistant.consumer.rerank;

import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import com.alibaba.cloud.ai.model.RerankResponseMetadata;
import com.alibaba.cloud.ai.model.RerankResultMetadata;
import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocalRerankModel implements RerankModel {

    private final RestClient restClient;
    private final String modelName;
    private final String rerankPath;
    private final String apiKey;
    private final Usage emptyUsage;

    public LocalRerankModel(RestClient restClient,
                            String modelName,
                            String rerankPath,
                            String apiKey,
                            Usage emptyUsage) {
        this.restClient = restClient;
        this.modelName = modelName;
        this.rerankPath = rerankPath;
        this.apiKey = apiKey;
        this.emptyUsage = emptyUsage;
    }

    @Override
    public RerankResponse call(RerankRequest request) {
        RestClient.RequestBodySpec requestBodySpec = restClient.post()
            .uri(rerankPath)
            .contentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            requestBodySpec.header("Authorization", "Bearer " + apiKey);
        }
        LocalRerankResponse response = requestBodySpec
            .body(new LocalRerankRequest(
                modelName,
                request.getQuery(),
                request.getInstructions().stream().map(Document::getText).toList()
            ))
            .retrieve()
            .body(LocalRerankResponse.class);

        List<DocumentWithScore> results = new ArrayList<>();
        if (response != null && response.results() != null) {
            List<Document> documents = request.getInstructions();
            for (LocalRerankResult item : response.results()) {
                if (item == null || item.index() == null) {
                    continue;
                }
                int idx = item.index();
                if (idx < 0 || idx >= documents.size()) {
                    continue;
                }
                DocumentWithScore documentWithScore = new DocumentWithScore();
                documentWithScore.setDocument(documents.get(idx));
                documentWithScore.setScore(item.relevanceScore());
                documentWithScore.setMetadata(new RerankResultMetadata());
                results.add(documentWithScore);
            }
        }

        Usage usage = response != null && response.usage() != null ? response.usage() : emptyUsage;
        String responseModel = response != null && response.model() != null && !response.model().isBlank()
            ? response.model()
            : modelName;
        return new RerankResponse(results, new RerankResponseMetadata(usage, Map.of("model", responseModel)));
    }

    private record LocalRerankRequest(String model, String query, List<String> documents) {}

    private record LocalRerankResponse(List<LocalRerankResult> results,
                                       UsageMetadata usage,
                                       String model) {}

    private record LocalRerankResult(Integer index,
                                     @JsonAlias("relevance_score") Double relevanceScore) {}

    private record UsageMetadata(@JsonAlias("prompt_tokens") Integer promptTokens,
                                 @JsonAlias("total_tokens") Integer totalTokens) implements Usage {
        @Override
        public Integer getPromptTokens() {
            return promptTokens != null ? promptTokens : 0;
        }

        @Override
        public Integer getCompletionTokens() {
            int total = totalTokens != null ? totalTokens : 0;
            int prompt = getPromptTokens();
            return Math.max(total - prompt, 0);
        }

        @Override
        public Integer getTotalTokens() {
            return totalTokens != null ? totalTokens : getPromptTokens();
        }

        @Override
        public Object getNativeUsage() {
            return this;
        }
    }
}
