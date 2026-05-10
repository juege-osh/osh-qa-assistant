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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
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
    @Resource
    private RerankModel rerankModel;
    @Resource
    private ChatClient chatClient;
    @Resource
    private UploadFileService uploadFileService;

    @Resource
    private InvokeRecordDetailService invokeRecordDetailService;

    @Resource
    private InvokeRecordService invokeRecordService;

    @Value("${spring.ai.dashscope.chat.options.model}")
    private String chatModelName;
    // 系统提示词模板
    private static final PromptTemplate SYSTEM_PROMPT_TEMPLATE = new PromptTemplate("""
			遵循下面的要求:
			{rule}
			## 限制
			### 所有的回答只能使用中文。
			""");
    private static final String NO_ANSWER = "问题超出知识库范围，我无法回答。";
    @Override
    public String doChat(SseEmitter sseEmitter, AppDO app, InvokeRecordBuilder builder) {
        StringBuilder retSb = new StringBuilder();
        InvokeRecordDetailBuilder detailBuilder4llm = initDetailBuilder4llm(builder);
        Flux<ChatResponse> fluxResponse = obtainResponse(builder, app);
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
                processError(sseEmitter,error,cdl,detailBuilder4llm);
            })
            .doOnComplete(() -> {
                // 所有的数据已经发送完毕
                processComplete(sseEmitter,cdl,lastResponseRef,detailBuilder4llm);
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
        } catch (IOException e) {
            log.error("send data to client error",e);
            sseEmitter.completeWithError(e);
        }
    }
    private void processComplete(SseEmitter sseEmitter, CountDownLatch cdl, AtomicReference<ChatResponse> lastResponseRef, InvokeRecordDetailBuilder detailBuilder4llm) {
        sendData(sseEmitter,ConsumerConstants.STREAM_END);
        // 获取最后一个响应
        ChatResponse lastResponse = lastResponseRef.get();
        if (lastResponse == null) {
            detailBuilder4llm.setStatus(InvokeStatusEnum.FAIL.getCode());
            detailBuilder4llm.setFailReason("响应为空");
        }else {
            Usage usage = lastResponse.getMetadata().getUsage();
            detailBuilder4llm.setStatus(InvokeStatusEnum.SUCCESS.getCode());
            detailBuilder4llm.setCostToken(Long.valueOf(usage.getTotalTokens()));
        }
        // 完成
        sseEmitter.complete();
        cdl.countDown();
    }

    private void processError(SseEmitter sseEmitter, Throwable error, CountDownLatch cdl, InvokeRecordDetailBuilder detailBuilder4llm) {
        log.error("chat appear error",error);
        detailBuilder4llm.setStatus(InvokeStatusEnum.FAIL.getCode());
        detailBuilder4llm.setFailReason(error.getMessage());
        sseEmitter.completeWithError(error);
        cdl.countDown();
    }

    private void processCancel(CountDownLatch cdl, InvokeRecordDetailBuilder detailBuilder4llm) {
        log.error("数据流被取消");
        detailBuilder4llm.setStatus(InvokeStatusEnum.FAIL.getCode());
        detailBuilder4llm.setFailReason("数据流被取消");
        cdl.countDown();
    }

    private void processNext(SseEmitter sseEmitter, ChatResponse response, StringBuilder retSb, AtomicReference<ChatResponse> lastResponseRef) {
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
    private Flux<ChatResponse> obtainResponse(InvokeRecordBuilder builder, AppDO app) {
        ChatDTO chatDTO = builder.getChatDto();
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient
            .prompt()
            // 系统提示词
            .system(getSystemPrompt(app))
            // 用户提示词
            .user(chatDTO.getUserInput());
        List<Document> documents = obtainDocuments(chatDTO.getUserInput(), app.getLibId(),builder);
        if (CollUtil.isEmpty(documents)) {
            // 超出知识库范围了
            if (YesNoEnum.Y.getCode().equals(app.getOutLibEnable())) {
                // 宽松模式
                // 设置会话id,用于记忆对话
                chatClientRequestSpec.advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,chatDTO.getConversationId()));
            }else {
                // 严格模式
                // 返回只包含一个元素的Flux对象
                Generation generation = new Generation(new AssistantMessage(NO_ANSWER));
                ChatResponse chatResponse = new ChatResponse(List.of(generation));
                return Flux.just(chatResponse);
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
        return chatClientRequestSpec.stream().chatResponse();
    }

    private String getSystemPrompt(AppDO app) {
        String rule;
        if (YesNoEnum.Y.getCode().equals(app.getOutLibEnable())) {
            rule = "优先从上下文中返回答案,如果上下文中找不到,就自行搜索资料后回答";
        }else {
            rule = """
                如果上下文信息中没有用户的答案,只能返回"问题超出知识库范围，我无法回答。"这几个字,
                不能有其他任何多余的回答。
                """;
        }
        Map<String,Object> promptParam = Map.of("rule",rule);
        return SYSTEM_PROMPT_TEMPLATE.render(promptParam);
    }

    private List<Document> obtainDocuments(String userInput, Long libId, InvokeRecordBuilder invokeRecordBuilder) {
        InvokeRecordContext.set(invokeRecordBuilder);
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                .query(userInput)
                // 默认只查4条
                .topK(CommonConstants.TOP_K)
                // 只查当前应用绑定的知识库的文档
                .filterExpression(CommonConstants.LIB_ID_STR_KEY + " == '" + libId +"'")
                .build();
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            documents = doRerank(userInput,documents);
            return documents;
        }finally {
            InvokeRecordContext.remove();
        }


    }

    /**
     * copied from {@link com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor}
     */
    protected List<Document> doRerank(String userInput, List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return documents;
        }

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
    }

}
