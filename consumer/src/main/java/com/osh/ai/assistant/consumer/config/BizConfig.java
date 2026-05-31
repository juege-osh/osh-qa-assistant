package com.osh.ai.assistant.consumer.config;

import cn.hutool.core.thread.NamedThreadFactory;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingProperties;
import com.alibaba.cloud.ai.autoconfigure.dashscope.ResolvedConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.model.RerankModel;
import com.osh.ai.assistant.consumer.aop.embedding.CustomDashScopeEmbeddingModel;
import com.osh.ai.assistant.consumer.rerank.LocalRerankModel;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.*;

import static com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionUtils.resolveConnectionProperties;


@Configuration
@Slf4j
public class BizConfig {
    @Resource
    private ChatClient.Builder chatClientBuilder;
    @Resource
    private ChatMemory chatMemory;

    @Value("${ai-assistant.chat.provider:openai}")
    private String chatProvider;

    @Value("${ai-assistant.chat.model:MiniMax-M2.7-highspeed}")
    private String chatModel;

    @Value("${ai-assistant.chat.temperature:0.7}")
    private Double chatTemperature;

    @Value("${ai-assistant.openai.enabled:true}")
    private boolean openAiEnabled;

    @Value("${ai-assistant.openai.base-url:https://v2.aicodee.com}")
    private String openAiBaseUrl;

    @Value("${ai-assistant.openai.api-key:}")
    private String openAiApiKey;

    @Value("${ai-assistant.openai.completions-path:/v1/chat/completions}")
    private String openAiCompletionsPath;

    @Value("${ai-assistant.embedding.provider:dashscope}")
    private String embeddingProvider;

    @Value("${ai-assistant.embedding.model:text-embedding-v4}")
    private String embeddingModelName;

    @Value("${ai-assistant.embedding.base-url:http://127.0.0.1:11434}")
    private String embeddingBaseUrl;

    @Value("${ai-assistant.embedding.api-key:ollama}")
    private String embeddingApiKey;

    @Value("${ai-assistant.embedding.dimensions:0}")
    private Integer embeddingDimensions;

    @Value("${ai-assistant.embedding.embeddings-path:/v1/embeddings}")
    private String embeddingPath;

    @Value("${ai-assistant.rerank.provider:dashscope}")
    private String rerankProvider;

    @Value("${ai-assistant.rerank.model:gte-rerank}")
    private String rerankModelName;

    @Value("${ai-assistant.rerank.base-url:http://127.0.0.1:18081}")
    private String rerankBaseUrl;

    @Value("${ai-assistant.rerank.api-key:local-rerank}")
    private String rerankApiKey;

    @Value("${ai-assistant.rerank.path:/rerank}")
    private String rerankPath;

    @Bean
    public ExecutorService executorService() {
        ThreadFactory namedThreadFactory = new NamedThreadFactory("chat-pool-thread-"
            , new ThreadGroup("chat-group")
            , false);
        return new ThreadPoolExecutor(30
            , 200
            , 60L
            , TimeUnit.SECONDS
            , new LinkedBlockingQueue<>(1024)
            , namedThreadFactory
            , new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * spring ai提供的与具体聊天模型无关的chat高级抽象的api,如果想使用具体模型的
     * 聊天api(有提供特定的功能)可以使用XxxChatModel,如:
     * {@link DashScopeChatModel#call}或{@link DashScopeChatModel#stream}
     */
    @Bean
    public ChatClient chatClient(ObjectProvider<WebClient.Builder> webClientBuilderProvider,
                                 ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                                 RetryTemplate retryTemplate,
                                 ResponseErrorHandler responseErrorHandler,
                                 ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        ChatClient.Builder builder = createChatClientBuilder(webClientBuilderProvider,
            restClientBuilderProvider, retryTemplate, responseErrorHandler, observationRegistryProvider);

        return builder
            .defaultAdvisors(
                // 打印每次聊天的输入和响应内容,配合配置日志输出级别为debug
                new SimpleLoggerAdvisor(),
                // 上下文记忆
                PromptChatMemoryAdvisor.builder(chatMemory).build()
                )
            .build();
    }

    private ChatClient.Builder createChatClientBuilder(ObjectProvider<WebClient.Builder> webClientBuilderProvider,
                                                       ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                                                       RetryTemplate retryTemplate,
                                                       ResponseErrorHandler responseErrorHandler,
                                                       ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        if (!openAiEnabled || !"openai".equalsIgnoreCase(chatProvider)) {
            log.info("Using DashScope chat client, provider={}", chatProvider);
            return chatClientBuilder;
        }

        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            log.warn("AI_ASSISTANT_OPENAI_API_KEY is empty, fallback to DashScope chat client");
            return chatClientBuilder;
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
            .baseUrl(openAiBaseUrl)
            .apiKey(openAiApiKey)
            .completionsPath(openAiCompletionsPath)
            .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
            .webClientBuilder(webClientBuilderProvider.getIfAvailable(WebClient::builder))
            .responseErrorHandler(responseErrorHandler)
            .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(chatModel)
            .temperature(chatTemperature)
            .build();

        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .retryTemplate(retryTemplate)
            .observationRegistry(observationRegistryProvider.getIfUnique(() -> ObservationRegistry.NOOP))
            .build();

        log.info("Using OpenAI-compatible chat client, baseUrl={}, model={}", openAiBaseUrl, chatModel);
        return ChatClient.builder(openAiChatModel);
    }

    /**
     * 从{@link com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration}拷贝,
     * 改动:
     * 修改返回值为{@link CustomDashScopeEmbeddingModel}
     */
    @Bean
    public CustomDashScopeEmbeddingModel dashscopeEmbeddingModel(DashScopeConnectionProperties commonProperties,
                                                                 DashScopeEmbeddingProperties embeddingProperties,
                                                                 ObjectProvider<WebClient.Builder> webClientBuilderProvider,
                                                                 ObjectProvider<RestClient.Builder> restClientBuilderProvider, RetryTemplate retryTemplate,
                                                                 ResponseErrorHandler responseErrorHandler, ObjectProvider<ObservationRegistry> observationRegistry,
                                                                 ObjectProvider<EmbeddingModelObservationConvention> observationConvention) {

        var dashScopeApi = dashscopeEmbeddingApi(commonProperties, embeddingProperties,
            restClientBuilderProvider.getIfAvailable(RestClient::builder),
            webClientBuilderProvider.getIfAvailable(WebClient::builder), responseErrorHandler);

        var embeddingModel = new CustomDashScopeEmbeddingModel(dashScopeApi, embeddingProperties.getMetadataMode(),
            embeddingProperties.getOptions(), retryTemplate,
            observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));

        observationConvention.ifAvailable(embeddingModel::setObservationConvention);

        return embeddingModel;
    }

    @Bean
    public EmbeddingModel embeddingModel(DashScopeConnectionProperties commonProperties,
                                         DashScopeEmbeddingProperties embeddingProperties,
                                         ObjectProvider<WebClient.Builder> webClientBuilderProvider,
                                         ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                                         RetryTemplate retryTemplate,
                                         ResponseErrorHandler responseErrorHandler,
                                         ObjectProvider<ObservationRegistry> observationRegistry,
                                         ObjectProvider<EmbeddingModelObservationConvention> observationConvention) {
        if (!"openai".equalsIgnoreCase(embeddingProvider)) {
            return dashscopeEmbeddingModel(commonProperties, embeddingProperties, webClientBuilderProvider,
                restClientBuilderProvider, retryTemplate, responseErrorHandler, observationRegistry, observationConvention);
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
            .baseUrl(embeddingBaseUrl)
            .apiKey(embeddingApiKey)
            .embeddingsPath(embeddingPath)
            .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
            .webClientBuilder(webClientBuilderProvider.getIfAvailable(WebClient::builder))
            .responseErrorHandler(responseErrorHandler)
            .build();

        OpenAiEmbeddingOptions.Builder optionsBuilder = OpenAiEmbeddingOptions.builder()
            .model(embeddingModelName);
        if (embeddingDimensions != null && embeddingDimensions > 0) {
            optionsBuilder.dimensions(embeddingDimensions);
        }
        OpenAiEmbeddingOptions options = optionsBuilder.build();

        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(
            openAiApi,
            MetadataMode.EMBED,
            options,
            retryTemplate,
            observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP)
        );
        observationConvention.ifAvailable(embeddingModel::setObservationConvention);
        log.info("Using OpenAI-compatible embedding model, baseUrl={}, model={}", embeddingBaseUrl, embeddingModelName);
        return embeddingModel;
    }

    @Bean(name = "rerankModel")
    @ConditionalOnExpression("'${ai-assistant.rerank.provider:dashscope}' == 'local' and '${ai-assistant.rerank.enabled:true}' == 'true'")
    public RerankModel rerankModel(ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
        RestClient.Builder builder = restClientBuilderProvider.getIfAvailable(RestClient::builder);
        if (builder == null) {
            builder = RestClient.builder();
        }
        log.info("Using local rerank model, baseUrl={}, model={}", rerankBaseUrl, rerankModelName);
        return new LocalRerankModel(
            builder.baseUrl(rerankBaseUrl).build(),
            rerankModelName,
            rerankPath,
            rerankApiKey,
            new EmptyUsage()
        );
    }

    private DashScopeApi dashscopeEmbeddingApi(DashScopeConnectionProperties commonProperties,
                                               DashScopeEmbeddingProperties embeddingProperties, RestClient.Builder restClientBuilder,
                                               WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
        ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties, embeddingProperties,
            "embedding");

        return DashScopeApi.builder()
            .apiKey(resolved.apiKey())
            .headers(resolved.headers())
            .baseUrl(resolved.baseUrl())
            .webClientBuilder(webClientBuilder)
            .workSpaceId(resolved.workspaceId())
            .restClientBuilder(restClientBuilder)
            .responseErrorHandler(responseErrorHandler)
            .build();
    }
}
