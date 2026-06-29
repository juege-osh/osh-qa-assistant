package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.bean.entity.UploadFileDO;
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
import com.osh.ai.assistant.consumer.service.AlertService;
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
import org.springframework.ai.openai.OpenAiChatOptions;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {
    private static final int REFERENCE_SNIPPET_LIMIT = 120;
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

    @Resource
    private AlertService alertService;

    @Value("${ai-assistant.chat.model:${spring.ai.dashscope.chat.options.model}}")
    private String chatModelName;

    @Value("${ai-assistant.chat.provider:openai}")
    private String chatProvider;

    @Value("${ai-assistant.chat.temperature:0.7}")
    private Double chatTemperature;

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
    private static final String WEAK_MATCH_NO_ANSWER = "当前找到了少量可能相关的资料，但还不足以支撑可靠回答。建议你补充更明确的制度名称、功能名称、流程节点或报错关键词后再试；如果这是知识库本应覆盖的问题，请联系知识库维护人补充内容。";
    private static final String KNOWLEDGE_TEMP_UNAVAILABLE = "当前知识库检索暂时不可用，无法基于可靠依据完成回答。建议你稍后重试；如果问题比较具体，也可以换一种更明确的问法后再试。";
    private static final String STREAM_INTERRUPTED = "当前回答因服务异常中断，以上内容可能不完整。建议你稍后重试，或换一种更具体的问法继续提问。";
    private static final String NO_ANSWER_FAIL_REASON = "未找到足够相关知识依据";
    private static final String WEAK_MATCH_FAIL_REASON = "仅召回弱相关内容，无法支撑可靠回答";
    private static final String HIGH_RISK_NO_ANSWER = "当前知识库里没有直接依据，而且这个问题涉及外部高风险信息，暂时不适合在无依据情况下直接作答。建议你补充明确的制度名称、流程来源，或联系对应专业负责人确认后再处理。";
    private static final String HIGH_RISK_NO_ANSWER_FAIL_REASON = "高风险外部问题缺少知识依据";
    private static final double HIGH_RISK_RAW_SCORE_THRESHOLD = 0.42D;

    private record ChatResponsePlan(Flux<ChatResponse> response, String failReason, String referencesMarkdown) {
        private static ChatResponsePlan ok(Flux<ChatResponse> response) {
            return new ChatResponsePlan(response, null, null);
        }

        private static ChatResponsePlan message(String message) {
            return new ChatResponsePlan(singleMessageResponse(message), null, null);
        }

        private static ChatResponsePlan fail(String message, String failReason) {
            return new ChatResponsePlan(singleMessageResponse(message), failReason, null);
        }

        private static ChatResponsePlan failWithReferences(String message, String failReason, String referencesMarkdown) {
            return new ChatResponsePlan(singleMessageResponse(message), failReason, referencesMarkdown);
        }

        private static ChatResponsePlan okWithReferences(Flux<ChatResponse> response, String referencesMarkdown) {
            return new ChatResponsePlan(response, null, referencesMarkdown);
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
        InvokeRecordDetailBuilder detailBuilder4llm = initDetailBuilder4llm(builder, app);
        ChatResponsePlan responsePlan = obtainResponse(builder, app);
        Flux<ChatResponse> fluxResponse = responsePlan.response();
        String referencesMarkdown = responsePlan.referencesMarkdown();
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
                processComplete(sseEmitter,cdl,lastResponseRef,detailBuilder4llm,responsePlan.failReason(), referencesMarkdown, retSb, closeOnComplete);
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

    @Override
    public String doChatSilently(AppDO app, InvokeRecordBuilder builder) {
        SseEmitter emitter = new SseEmitter(0L);
        return doChat(emitter, app, builder, true);
    }

    private InvokeRecordDetailBuilder initDetailBuilder4llm(InvokeRecordBuilder builder, AppDO app) {
        return InvokeRecordDetailBuilder.builder()
            .setInvokeRecordId(builder.getId())
            .setModelName(resolveRuntimeModelName(app))
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
                                 InvokeRecordDetailBuilder detailBuilder4llm, String failReason,
                                 String referencesMarkdown, StringBuilder retSb, boolean closeOnComplete) {
        // 获取最后一个响应
        ChatResponse lastResponse = lastResponseRef.get();
        if (lastResponse == null) {
            detailBuilder4llm.setStatus(InvokeStatusEnum.FAIL.getCode());
            detailBuilder4llm.setFailReason("响应为空");
        }else if (StrUtil.isNotBlank(failReason)) {
            detailBuilder4llm.setStatus(InvokeStatusEnum.FAIL.getCode());
            detailBuilder4llm.setFailReason(failReason);
            appendReferencesIfPresent(sseEmitter, retSb, referencesMarkdown);
        }else {
            Usage usage = lastResponse.getMetadata().getUsage();
            detailBuilder4llm.setStatus(InvokeStatusEnum.SUCCESS.getCode());
            detailBuilder4llm.setCostToken(Long.valueOf(usage.getTotalTokens()));
            appendReferencesIfPresent(sseEmitter, retSb, referencesMarkdown);
        }
        sendData(sseEmitter,ConsumerConstants.STREAM_END);
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
        RetrievalResult retrievalResult;
        try {
            retrievalResult = obtainDocuments(chatDTO.getUserInput(), app, builder);
        } catch (Exception e) {
            String failReason = buildKnowledgeRetrievalFailReason(e);
            log.error("knowledge retrieval unavailable, requestId={}, appId={}, libId={}, userInput={}",
                chatDTO.getRequestId(), app.getId(), app.getLibId(), chatDTO.getUserInput(), e);
            alertService.notifyOnce("knowledge-retrieval-unavailable-" + app.getLibId(),
                "知识库检索链路异常",
                "请求期知识库检索失败。\n请求ID: " + chatDTO.getRequestId()
                    + "\n应用ID: " + app.getId()
                    + "\n知识库ID: " + app.getLibId()
                    + "\n用户问题: " + chatDTO.getUserInput()
                    + "\n异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return ChatResponsePlan.fail(KNOWLEDGE_TEMP_UNAVAILABLE, failReason);
        }
        List<Document> documents = retrievalResult.documents();
        List<Document> rawDocuments = retrievalResult.rawDocuments();
        boolean knowledgeBound = app.getLibId() != null;
        boolean highRiskOutOfKnowledgeQuestion = knowledgeBound && isHighRiskOutOfKnowledgeQuestion(chatDTO.getUserInput());
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient
            .prompt()
            // 系统提示词
            .system(getSystemPrompt(app, knowledgeBound, CollUtil.isNotEmpty(documents), CollUtil.isNotEmpty(rawDocuments), highRiskOutOfKnowledgeQuestion))
            // 用户提示词
            .user(chatDTO.getUserInput());
        applyAppChatOptions(chatClientRequestSpec, app);
        if (highRiskOutOfKnowledgeQuestion && shouldBlockHighRiskAnswer(documents, rawDocuments)) {
            return ChatResponsePlan.failWithReferences(
                HIGH_RISK_NO_ANSWER,
                HIGH_RISK_NO_ANSWER_FAIL_REASON,
                buildPossibleReferencesMarkdown(rawDocuments)
            );
        }
        if (CollUtil.isEmpty(documents)) {
            // 超出知识库范围了
            if (YesNoEnum.Y.getCode().equals(app.getOutLibEnable())) {
                // 宽松模式
                // 设置会话id,用于记忆对话
                chatClientRequestSpec.advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,chatDTO.getConversationId()));
                String referencesMarkdown = knowledgeBound ? buildPossibleReferencesMarkdown(rawDocuments) : null;
                return ChatResponsePlan.okWithReferences(chatClientRequestSpec.stream().chatResponse(), referencesMarkdown);
            }else {
                // 严格模式
                String visibleMessage = CollUtil.isNotEmpty(rawDocuments) ? WEAK_MATCH_NO_ANSWER : NO_ANSWER;
                String failReason = CollUtil.isNotEmpty(rawDocuments) ? WEAK_MATCH_FAIL_REASON : NO_ANSWER_FAIL_REASON;
                String referencesMarkdown = buildPossibleReferencesMarkdown(rawDocuments);
                return ChatResponsePlan.failWithReferences(visibleMessage, failReason, referencesMarkdown);
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
        return ChatResponsePlan.okWithReferences(chatClientRequestSpec.stream().chatResponse(), buildReferencesMarkdown(documents));
    }

    private String getSystemPrompt(AppDO app, boolean knowledgeBound, boolean hasReliableDocs, boolean hasWeakMatch,
                                   boolean highRiskOutOfKnowledgeQuestion) {
        String rule;
        boolean allowOutOfLib = YesNoEnum.Y.getCode().equals(app.getOutLibEnable());
        if (!allowOutOfLib) {
            rule = """
                如果上下文信息中没有足够依据,请明确说明当前未在知识库中找到足够相关依据,
                暂时无法可靠回答,并建议用户换一种更具体的问法或联系知识库维护人。
                不要编造答案,不要假装已经从知识库中找到了依据。
                如果上下文中有依据,优先按下面结构回答:
                1. 先用 1 到 2 句话给出直接结论。
                2. 再列出支撑该结论的依据、规则或关键步骤。
                3. 如果问题本身信息不足,指出还缺什么信息才能更准确回答。
                如果用户问的是“从哪里开始”“先后步骤”“先看什么”这类任务指导问题,而上下文里已经有通用起步建议或推荐顺序,
                请先直接给出最小可执行建议或步骤,再补充还需要用户确认的信息,不要只回复“问题太模糊”。
                如果用户问的是知识库覆盖范围、主要主题、当前能回答什么,而上下文里已经有覆盖范围说明,
                请直接概括覆盖主题,不要因为问题宽泛就拒答。
                """;
        } else if (!knowledgeBound) {
            rule = """
                当前应用未绑定知识库,可以直接给出通用帮助。
                如果问题是“从哪里开始”“下一步怎么做”“先看什么”,优先给出最小可执行建议或步骤。
                如果问题涉及明确事实、规则、日期、金额等确定性结论,请不要把不确定内容说成已确认事实,
                需要把不确定性直接说明。
                """;
        } else if (highRiskOutOfKnowledgeQuestion) {
            rule = """
                当前问题涉及外部高风险信息。
                只有在上下文里有直接、明确、可支撑的知识依据时，才可以回答。
                如果没有直接依据，或者只召回到弱相关资料，必须明确说明“当前知识库没有足够依据支持这个回答”，
                并建议用户补充明确制度名称、流程来源，或联系对应专业负责人确认。
                不要补充外部机构、官网、法律、税务、医疗、投资等泛化建议，
                也不要把常识性建议包装成当前知识库已确认的结论。
                """;
        } else if (hasReliableDocs) {
            rule = """
                优先基于上下文返回答案。
                如果上下文中已有依据,先给出简短结论,再补充对应依据、关键步骤或注意事项。
                如果用户问的是知识库覆盖范围、主要主题、当前能回答什么,请直接概括当前知识库覆盖内容。
                如果用户问的是“从哪里开始”“下一步怎么做”“先看什么”,请优先给出最小可执行建议或步骤。
                如果确实需要补充通用建议,请明确说明哪些内容属于通用建议,不是当前知识库的直接依据。
                不要把通用建议伪装成知识库已经明确给出的结论。
                """;
        } else if (hasWeakMatch) {
            rule = """
                当前只找到了少量可能相关但不足以支撑结论的资料。
                你可以继续提供帮助,但必须先明确说明“下面属于通用建议,不是来自当前知识库的直接依据”。
                如果问题是任务起步、流程梳理、排查方向这类问题,先给最小可执行建议,再提醒用户补充更明确关键词。
                如果问题需要明确制度结论、外部事实、规则口径、日期金额等确定依据,不要给出看似确定的答案,
                而要说明当前缺少可靠依据,建议用户补充制度名称、流程名称、报错关键词或联系知识库维护人。
                不要把弱相关资料说成已经足够支撑结论。
                """;
        }else {
            rule = """
                当前这次提问没有命中到足够可靠的知识依据。
                你可以继续提供帮助,但必须先明确说明“下面属于通用建议,不是来自当前知识库的直接依据”。
                如果问题是“从哪里开始”“下一步怎么做”“先看什么”这类任务指导,请先给最小可执行建议或步骤。
                如果问题是制度结论、外部事实、规则口径、日期金额等需要明确依据的内容,不要给出确定答案,
                而要说明当前缺少可靠依据,建议用户补充更具体的制度名称、流程名称或联系知识库维护人。
                不要把通用建议写成知识库已经确认的结论。
                """;
        }
        String basePrompt = SYSTEM_PROMPT_TEMPLATE.render(Map.of("rule", rule));
        if (app == null || StrUtil.isBlank(app.getCustomPrompt())) {
            return basePrompt;
        }
        return basePrompt + "\n## 应用补充要求\n" + app.getCustomPrompt().trim();
    }

    private void applyAppChatOptions(ChatClient.ChatClientRequestSpec requestSpec, AppDO app) {
        if (app == null || StrUtil.isBlank(app.getChatModel())) {
            return;
        }
        String targetModel = app.getChatModel().trim();
        if ("openai".equalsIgnoreCase(chatProvider)) {
            requestSpec.options(OpenAiChatOptions.builder()
                .model(targetModel)
                .temperature(chatTemperature)
                .build());
            return;
        }
        requestSpec.options(DashScopeChatOptions.builder()
            .model(targetModel)
            .temperature(chatTemperature)
            .build());
    }

    private String resolveRuntimeModelName(AppDO app) {
        if (app != null && StrUtil.isNotBlank(app.getChatModel())) {
            return app.getChatModel().trim();
        }
        return chatModelName;
    }

    private RetrievalResult obtainDocuments(String userInput, AppDO app, InvokeRecordBuilder invokeRecordBuilder) {
        if (app.getLibId() == null) {
            return new RetrievalResult(List.of(), List.of());
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
            List<Document> rawDocuments = vectorStore.similaritySearch(searchRequest);
            List<Document> documents = doRerank(userInput, rawDocuments);
            return new RetrievalResult(documents, rawDocuments);
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

    private String buildReferencesMarkdown(List<Document> documents) {
        if (CollUtil.isEmpty(documents)) {
            return null;
        }
        Map<String, UploadFileDO> uploadFileMap = buildUploadFileMap(documents);
        if (uploadFileMap.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder("---\n**参考来源**\n");
        int limit = Math.min(documents.size(), 3);
        for (int i = 0; i < limit; i++) {
            Document document = documents.get(i);
            UploadFileDO uploadFileDO = uploadFileMap.get(document.getId());
            if (uploadFileDO == null) {
                continue;
            }
            builder.append(i + 1)
                .append(". ")
                .append(uploadFileDO.getFileName())
                .append("\n");
            builder.append("   片段: ")
                .append(buildSnippet(document.getText()))
                .append("\n");
        }
        return builder.toString();
    }

    private String buildPossibleReferencesMarkdown(List<Document> documents) {
        if (CollUtil.isEmpty(documents)) {
            return null;
        }
        Map<String, UploadFileDO> uploadFileMap = buildUploadFileMap(documents);
        if (uploadFileMap.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder("---\n**可补充核对的资料**\n");
        builder.append("以下资料与当前问题可能接近,但暂不足以支撑可靠回答:\n");
        int limit = Math.min(documents.size(), 3);
        for (int i = 0; i < limit; i++) {
            Document document = documents.get(i);
            UploadFileDO uploadFileDO = uploadFileMap.get(document.getId());
            if (uploadFileDO == null) {
                continue;
            }
            builder.append(i + 1)
                .append(". ")
                .append(uploadFileDO.getFileName())
                .append("\n");
            builder.append("   片段: ")
                .append(buildSnippet(document.getText()))
                .append("\n");
        }
        return builder.toString();
    }

    private Map<String, UploadFileDO> buildUploadFileMap(List<Document> documents) {
        List<UploadFileDO> uploadFiles = uploadFileService.selectByDocIds(documents.stream().map(Document::getId).toList());
        if (CollUtil.isEmpty(uploadFiles)) {
            return Map.of();
        }
        Map<String, UploadFileDO> ret = new LinkedHashMap<>();
        for (Document document : documents) {
            for (UploadFileDO uploadFileDO : uploadFiles) {
                if (StrUtil.contains(uploadFileDO.getDocIds(), document.getId())) {
                    ret.putIfAbsent(document.getId(), uploadFileDO);
                    break;
                }
            }
        }
        return ret;
    }

    private String buildSnippet(String text) {
        if (StrUtil.isBlank(text)) {
            return "暂无可展示片段";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').replace('\n', ' ').trim();
        if (normalized.length() <= REFERENCE_SNIPPET_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, REFERENCE_SNIPPET_LIMIT) + "...";
    }

    private boolean isHighRiskOutOfKnowledgeQuestion(String userInput) {
        if (StrUtil.isBlank(userInput)) {
            return false;
        }
        return containsAnyIgnoreCase(userInput,
            "报税", "税务", "纳税", "所得税", "法律", "合同", "仲裁", "诉讼",
            "医疗", "诊断", "处方", "药物", "投资", "理财", "证券", "股票", "基金",
            "贷款", "利率", "签证", "移民", "处罚", "合规");
    }

    private boolean shouldBlockHighRiskAnswer(List<Document> documents, List<Document> rawDocuments) {
        if (CollUtil.isEmpty(documents)) {
            return true;
        }
        if (CollUtil.isEmpty(rawDocuments)) {
            return true;
        }
        double bestRawScore = rawDocuments.stream()
            .filter(Objects::nonNull)
            .map(Document::getScore)
            .filter(Objects::nonNull)
            .max(Double::compareTo)
            .orElse(0D);
        return bestRawScore < HIGH_RISK_RAW_SCORE_THRESHOLD;
    }

    private boolean containsAnyIgnoreCase(String text, String... keywords) {
        for (String keyword : keywords) {
            if (StrUtil.containsIgnoreCase(text, keyword)) {
                return true;
            }
        }
        return false;
    }

    private void appendReferencesIfPresent(SseEmitter sseEmitter, StringBuilder retSb, String referencesMarkdown) {
        if (StrUtil.isBlank(referencesMarkdown) || StrUtil.isBlank(retSb.toString())) {
            return;
        }
        String referencesBlock = StrUtil.endWith(retSb.toString(), "\n")
            ? "\n" + referencesMarkdown
            : "\n\n" + referencesMarkdown;
        retSb.append(referencesBlock);
        sendData(sseEmitter, referencesBlock);
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

    private record RetrievalResult(List<Document> documents, List<Document> rawDocuments) {
    }

}
