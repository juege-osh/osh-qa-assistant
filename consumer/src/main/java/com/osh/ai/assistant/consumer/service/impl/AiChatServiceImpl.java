package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.enums.InvokeStatusEnum;
import com.osh.ai.assistant.common.enums.YesNoEnum;
import com.osh.ai.assistant.consumer.advisor.QueryAugmenterAdvisor;
import com.osh.ai.assistant.consumer.bean.dto.ChatDTO;
import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;
import com.osh.ai.assistant.consumer.builder.InvokeRecordDetailBuilder;
import com.osh.ai.assistant.consumer.constants.ConsumerConstants;
import com.osh.ai.assistant.consumer.context.InvokeRecordContext;
import com.osh.ai.assistant.consumer.service.AiChatService;
import com.osh.ai.assistant.consumer.service.InvokeRecordDetailService;
import com.osh.ai.assistant.consumer.service.InvokeRecordService;
import com.osh.ai.assistant.consumer.service.UploadFileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {
    @Resource
    private VectorStore vectorStore;
    @Autowired(required = false)
    private RerankModel rerankModel;
    @Resource
    private ChatClient chatClient;
    @Resource
    private UploadFileService uploadFileService;

    @Resource
    private InvokeRecordDetailService invokeRecordDetailService;

    @Resource
    private InvokeRecordService invokeRecordService;

    @Value("${ai-assistant.chat.model:${spring.ai.dashscope.chat.options.model}}")
    private String chatModelName;

    @Value("${ai-assistant.rerank.enabled:true}")
    private boolean rerankEnabled;
    // 系统提示词模板
    private static final PromptTemplate SYSTEM_PROMPT_TEMPLATE = new PromptTemplate("""
			遵循下面的要求:
			{rule}
			## 限制
			### 所有的回答只能使用中文。
			""");
    private static final String NO_ANSWER = "当前未在知识库中找到足够相关依据，暂时无法可靠回答这个问题。建议你换一种更具体的问法，补充关键词、制度名称或流程名称后再试；如果这是一个应该能回答的问题，请联系知识库维护人补充内容。";
    private static final String KNOWLEDGE_TEMP_UNAVAILABLE = "当前知识库检索暂时不可用，无法基于可靠依据完成回答。建议你稍后重试；如果问题比较具体，也可以换一种更明确的问法后再试。";
    private static final String STREAM_INTERRUPTED = "当前回答因服务异常中断，以上内容可能不完整。建议你稍后重试，或换一种更具体的问法继续提问。";

    private record ChatResponsePlan(Flux<ChatResponse> response, String failReason) {
        private static ChatResponsePlan ok(Flux<ChatResponse> response) {
            return new ChatResponsePlan(response, null);
        }

        private static ChatResponsePlan message(String message) {
            return new ChatResponsePlan(singleMessageResponse(message), null);
        }

        private static ChatResponsePlan fail(String message, String failReason) {
            return new ChatResponsePlan(singleMessageResponse(message), failReason);
        }

        private static Flux<ChatResponse> singleMessageResponse(String message) {
            Generation generation = new Generation(new AssistantMessage(message));
            ChatResponse chatResponse = new ChatResponse(List.of(generation));
            return Flux.just(chatResponse);
        }
    }

    @Override
    public String doChat(SseEmitter sseEmitter, AppDO app, InvokeRecordBuilder builder, boolean closeOnComplete) {
        StringBuilder retSb = new StringBuilder();
        InvokeRecordDetailBuilder detailBuilder4llm = initDetailBuilder4llm(builder);
        ChatResponsePlan responsePlan = obtainResponse(builder, app);
        Flux<ChatResponse> fluxResponse = responsePlan.response();
        CountDownLatch cdl = new CountDownLatch(1);
        // 记录每次响应结果
        AtomicReference<ChatResponse> lastResponseRef = new AtomicReference<>();
        fluxResponse
            // 只要有结果返回就触发这个方法
            .doOnNext(response -> {
                processNext(sseEmitter,response,retSb,lastResponseRef);
            })
            .doOnCancel(() -> {
                processCancel(cdl,detailBuilder4llm);
            })
            .doOnError(error -> {
                processError(sseEmitter,error,cdl,detailBuilder4llm,retSb);
            })
            .doOnComplete(() -> {
                // 所有的数据已经发送完毕
                processComplete(sseEmitter,cdl,lastResponseRef,detailBuilder4llm,responsePlan.failReason(), closeOnComplete);
            })
            .subscribe();
        try {
            boolean await = cdl.await(5, TimeUnit.MINUTES);
            if (!await) {
                detailBuilder4llm.setStatus(InvokeStatusEnum.FAIL.getCode());
                detailBuilder4llm.setFailReason("等待超时");
            }
            // 设置响应结果
            detailBuilder4llm.setAssistantMessage(retSb.toString());
            invokeRecordDetailService.add(detailBuilder4llm);
            invokeRecordService.record(builder);
        }catch (Exception e) {
            log.error("await error",e);
        }
        return retSb.toString();
    }

    private InvokeRecordDetailBuilder initDetailBuilder4llm(InvokeRecordBuilder builder) {
        return InvokeRecordDetailBuilder.builder()
            .setInvokeRecordId(builder.getId())
            .setModelName(chatModelName)
            .setStartTime(new Date())
            // 设置查询词
            .setUserInput(builder.getChatDto().getUserInput());
    }

    /**
     * 发送数据到客户端
     */
    private void sendData(SseEmitter sseEmitter,String content) {
        try {
            sseEmitter.send(SseEmitter.event().data(content));
        } catch (Exception e) {
            if (isClientDisconnect(e)) {
                log.info("sse client disconnected while sending response");
            } else {
                log.error("send data to client error",e);
            }
            sseEmitter.completeWithError(e);
        }
    }
    private void processComplete(SseEmitter sseEmitter, CountDownLatch cdl, AtomicReference<ChatResponse> lastResponseRef,
                                 InvokeRecordDetailBuilder detailBuilder4llm, String failReason, boolean closeOnComplete) {
        sendData(sseEmitter,ConsumerConstants.STREAM_END);
        // 获取最后一个响应
        ChatResponse lastResponse = lastResponseRef.get();
        if (lastResponse == null) {
            detailBuilder4llm.setStatus(InvokeStatusEnum.FAIL.getCode());
            detailBuilder4llm.setFailReason("响应为空");
        }else if (StrUtil.isNotBlank(failReason)) {
            detailBuilder4llm.setStatus(InvokeStatusEnum.FAIL.getCode());
            detailBuilder4llm.setFailReason(failReason);
        }else {
            Usage usage = lastResponse.getMetadata().getUsage();
            detailBuilder4llm.setStatus(InvokeStatusEnum.SUCCESS.getCode());
            detailBuilder4llm.setCostToken(Long.valueOf(usage.getTotalTokens()));
        }
        if (closeOnComplete) {
            sseEmitter.complete();
        }
        cdl.countDown();
    }

    private void processError(SseEmitter sseEmitter, Throwable error, CountDownLatch cdl,
                              InvokeRecordDetailBuilder detailBuilder4llm, StringBuilder retSb) {
        log.error("chat appear error",error);
        detailBuilder4llm.setStatus(InvokeStatusEnum.FAIL.getCode());
        detailBuilder4llm.setFailReason(error.getMessage());
        String visibleMessage = StrUtil.isBlank(retSb.toString()) ? KNOWLEDGE_TEMP_UNAVAILABLE : STREAM_INTERRUPTED;
        if (StrUtil.isNotBlank(retSb.toString()) && !StrUtil.endWith(retSb.toString(), "\n")) {
            retSb.append("\n\n");
        }
        retSb.append(visibleMessage);
        sendData(sseEmitter, visibleMessage);
        sendData(sseEmitter, ConsumerConstants.STREAM_END);
        sseEmitter.complete();
        cdl.countDown();
    }

    private void processCancel(CountDownLatch cdl, InvokeRecordDetailBuilder detailBuilder4llm) {
        log.error("数据流被取消");
        detailBuilder4llm.setStatus(InvokeStatusEnum.FAIL.getCode());
        detailBuilder4llm.setFailReason("数据流被取消");
        cdl.countDown();
    }

    private void processNext(SseEmitter sseEmitter, ChatResponse response, StringBuilder retSb, AtomicReference<ChatResponse> lastResponseRef) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return;
        }
        // 记录每个响应,最后一个响应会被保留
        lastResponseRef.set(response);
        // 服务端的响应内容
        String text = response.getResult().getOutput().getText();
        if (StrUtil.isBlank(text)) {
            return;
        }
        retSb.append(text);
        sendData(sseEmitter,text);
    }

    /**
     * 获取响应
     */
    private ChatResponsePlan obtainResponse(InvokeRecordBuilder builder, AppDO app) {
        ChatDTO chatDTO = builder.getChatDto();
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient
            .prompt()
            // 系统提示词
            .system(getSystemPrompt(app))
            // 用户提示词
            .user(chatDTO.getUserInput());
        List<Document> documents;
        try {
            documents = obtainDocuments(chatDTO.getUserInput(), app,builder);
        } catch (Exception e) {
            String failReason = buildKnowledgeRetrievalFailReason(e);
            log.error("knowledge retrieval unavailable, requestId={}, appId={}, libId={}, userInput={}",
                chatDTO.getRequestId(), app.getId(), app.getLibId(), chatDTO.getUserInput(), e);
            return ChatResponsePlan.fail(KNOWLEDGE_TEMP_UNAVAILABLE, failReason);
        }
        if (CollUtil.isEmpty(documents)) {
            // 超出知识库范围了
            if (YesNoEnum.Y.getCode().equals(app.getOutLibEnable())) {
                // 宽松模式
                // 设置会话id,用于记忆对话
                chatClientRequestSpec.advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,chatDTO.getConversationId()));
            }else {
                // 严格模式
                // 返回只包含一个元素的Flux对象
                return ChatResponsePlan.message(NO_ANSWER);
            }
        }else {
            // 有结果,通过自定义advisor放到上下文中
            chatClientRequestSpec.advisors(advisorSpec -> {
                advisorSpec
                    .advisors(new QueryAugmenterAdvisor(documents))
                    .param(ChatMemory.CONVERSATION_ID,chatDTO.getConversationId());
            });
            uploadFileService.incrRecallCount(documents.stream().map(Document::getId).toList());
        }
        return ChatResponsePlan.ok(chatClientRequestSpec.stream().chatResponse());
    }

    private String getSystemPrompt(AppDO app) {
        String rule;
        if (YesNoEnum.Y.getCode().equals(app.getOutLibEnable())) {
            rule = "优先从上下文中返回答案,如果上下文中找不到,就自行搜索资料后回答";
        }else {
            rule = """
                如果上下文信息中没有足够依据,请明确说明当前未在知识库中找到足够相关依据,
                暂时无法可靠回答,并建议用户换一种更具体的问法或联系知识库维护人。
                不要编造答案,不要假装已经从知识库中找到了依据。
                """;
        }
        Map<String,Object> promptParam = Map.of("rule",rule);
        return SYSTEM_PROMPT_TEMPLATE.render(promptParam);
    }

    private List<Document> obtainDocuments(String userInput, AppDO app, InvokeRecordBuilder invokeRecordBuilder) {
        if (app.getLibId() == null) {
            return List.of();
        }
        InvokeRecordContext.set(invokeRecordBuilder);
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                .query(userInput)
                // 默认只查4条
                .topK(CommonConstants.TOP_K)
                // 只查当前应用绑定的知识库的文档
                .filterExpression(CommonConstants.LIB_ID_STR_KEY + " == '" + app.getLibId() +"'")
                .build();
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            documents = doRerank(userInput,documents);
            return documents;
        }finally {
            InvokeRecordContext.remove();
        }


    }

    private String buildKnowledgeRetrievalFailReason(Exception e) {
        String message = e.getMessage();
        String reason = "知识库检索暂不可用: " + e.getClass().getSimpleName();
        if (StrUtil.isNotBlank(message)) {
            reason += ": " + message;
        }
        if (reason.length() > 500) {
            return reason.substring(0, 500);
        }
        return reason;
    }

    private boolean isClientDisconnect(Exception e) {
        if (e instanceof AsyncRequestNotUsableException) {
            return true;
        }
        Throwable current = e;
        while (current != null) {
            if (current instanceof IOException && StrUtil.containsIgnoreCase(current.getMessage(), "Broken pipe")) {
                return true;
            }
            if (StrUtil.containsIgnoreCase(current.getClass().getName(), "ClientAbortException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * copied from {@link com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor}
     */
    protected List<Document> doRerank(String userInput, List<Document> documents) {
        if (!rerankEnabled || rerankModel == null || CollectionUtils.isEmpty(documents)) {
            return documents;
        }

        try {
            var rerankRequest = new RerankRequest(userInput, documents);
            // 执行重排序
            RerankResponse response = rerankModel.call(rerankRequest);
            if (response == null || response.getResults() == null) {
                return documents;
            }

            return response.getResults()
                .stream()
                .filter(doc -> doc != null && doc.getScore() >= ConsumerConstants.MIN_SCORE)
                .sorted(Comparator.comparingDouble(DocumentWithScore::getScore).reversed())
                .map(DocumentWithScore::getOutput)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("rerank failed, fallback to vector search results", e);
            return documents;
        }
    }

}
