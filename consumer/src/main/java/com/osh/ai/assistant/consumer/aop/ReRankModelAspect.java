package com.osh.ai.assistant.consumer.aop;

import com.alibaba.cloud.ai.model.RerankResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 重排序模型切面,统计token用量
 */
@Component
@Slf4j
@Aspect
public class ReRankModelAspect {

    @Resource
    private InvokeRecordDetailService invokeRecordDetailService;

    @Value("${spring.ai.dashscope.rerank.options.model}")
    private String modelName;

    @Pointcut("execution(* com.alibaba.cloud.ai.model.RerankModel.call(com.alibaba.cloud.ai.model.RerankRequest))")
    public void pc() {}

    @Around("pc()")
    public Object aroundCall(ProceedingJoinPoint pjp) throws Throwable {
        InvokeRecordBuilder invokeRecordBuilder = InvokeRecordContext.get();
        InvokeRecordDetailBuilder detailBuilder = InvokeRecordDetailBuilder.builder()
            .setInvokeRecordId(invokeRecordBuilder.getId())
            .setUserInput(invokeRecordBuilder.getChatDto().getUserInput())
            .setModelName(modelName)
            .setStartTime(new Date());
        try {
            RerankResponse rerankResponse = (RerankResponse) pjp.proceed();
            // 获取token统计
            Usage usage = rerankResponse.getMetadata().getUsage();
            detailBuilder.setCostToken(Long.valueOf(usage.getTotalTokens()));
            detailBuilder.setStatus(InvokeStatusEnum.SUCCESS.getCode());
            detailBuilder.setAssistantMessage("重排后(未过滤)结果数:" + rerankResponse.getResults().size());
            return rerankResponse;
        }catch (Throwable t) {
            log.error("rerank model call error",t);
            detailBuilder.setStatus(InvokeStatusEnum.FAIL.getCode());
            detailBuilder.setFailReason(t.getMessage());
            throw t;
        }finally {
            invokeRecordDetailService.add(detailBuilder);
        }
    }
}
