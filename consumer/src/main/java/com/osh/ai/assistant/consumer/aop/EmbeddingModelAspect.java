package com.osh.ai.assistant.consumer.aop;

import com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec;
import com.osh.ai.assistant.common.enums.InvokeStatusEnum;
import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;
import com.osh.ai.assistant.consumer.builder.InvokeRecordDetailBuilder;
import com.osh.ai.assistant.consumer.context.InvokeRecordContext;
import com.osh.ai.assistant.consumer.service.InvokeRecordDetailService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 向量模型切面,统计token用量
 */
@Component
@Slf4j
@Aspect
public class EmbeddingModelAspect {

    @Resource
    private InvokeRecordDetailService invokeRecordDetailService;

    @Value("${spring.ai.dashscope.embedding.options.model}")
    private String modelName;

    @Pointcut("execution(* org.springframework.ai.embedding.EmbeddingModel.call(..))")
    public void pc() {}

    @Around("pc()")
    public Object aroundCall(ProceedingJoinPoint pjp) throws Throwable {
        InvokeRecordBuilder invokeRecordBuilder = InvokeRecordContext.get();
        if (invokeRecordBuilder == null) {
            // 非智能问答场景
            return pjp.proceed();
        }
        InvokeRecordDetailBuilder detailBuilder = InvokeRecordDetailBuilder.builder()
            .setInvokeRecordId(invokeRecordBuilder.getId())
            .setUserInput(invokeRecordBuilder.getChatDto().getUserInput())
            .setModelName(modelName)
            .setStartTime(new Date());
        try {
            EmbeddingResponse embeddingResponse = (EmbeddingResponse) pjp.proceed();
            // 获取token统计
            Usage usage = embeddingResponse.getMetadata().getUsage();
            // 这里获取不到
//            Integer totalTokens = usage.getTotalTokens();
            // 通过这种方式能获取到
            DashScopeApiSpec.EmbeddingUsage nativeUsage = (DashScopeApiSpec.EmbeddingUsage) usage.getNativeUsage();
            detailBuilder.setCostToken(nativeUsage.totalTokens());
            detailBuilder.setStatus(InvokeStatusEnum.SUCCESS.getCode());
            return embeddingResponse;
        }catch (Throwable t) {
            log.error("embedding model call error",t);
            detailBuilder.setStatus(InvokeStatusEnum.FAIL.getCode());
            detailBuilder.setFailReason(t.getMessage());
            throw t;
        }finally {
            invokeRecordDetailService.add(detailBuilder);
        }
    }
}
