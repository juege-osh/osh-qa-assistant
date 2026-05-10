package com.osh.ai.assistant.consumer.config;

import cn.hutool.core.thread.NamedThreadFactory;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingProperties;
import com.alibaba.cloud.ai.autoconfigure.dashscope.ResolvedConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.osh.ai.assistant.consumer.aop.embedding.CustomDashScopeEmbeddingModel;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
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
    public ChatClient chatClient() {
        return chatClientBuilder
            .defaultAdvisors(
                // 打印每次聊天的输入和响应内容,配合配置日志输出级别为debug
                new SimpleLoggerAdvisor(),
                // 上下文记忆
                PromptChatMemoryAdvisor.builder(chatMemory).build()
                )
            .build();
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
