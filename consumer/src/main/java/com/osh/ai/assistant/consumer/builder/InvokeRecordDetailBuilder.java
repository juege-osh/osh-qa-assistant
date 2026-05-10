package com.osh.ai.assistant.consumer.builder;

import com.osh.ai.assistant.common.bean.entity.InvokeRecordDetailDO;
import com.osh.ai.assistant.common.enums.InvokeStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;


@Accessors(chain = true)
@Data
public class InvokeRecordDetailBuilder {
    /**
     * invoke_record表的主键
     */
    private Long invokeRecordId;
    /**
     * 模型名称
     */
    private String modelName;
    /**
     * 消耗token数
     */
    private Long costToken;
    /**
     * 状态,0:失败,1:成功
     * @see InvokeStatusEnum
     */
    private Integer status;
    /**
     * 失败原因
     */
    private String failReason;
    /**
     * 一次调用的开始时间
     */
    private Date startTime;
    /**
     * 用户输入
     */
    private String userInput;
    /**
     * 响应结果
     */
    private String assistantMessage;

    private InvokeRecordDetailBuilder(){}
    public static InvokeRecordDetailBuilder builder() {
        return new InvokeRecordDetailBuilder();
    }

    public InvokeRecordDetailDO build() {
        InvokeRecordDetailDO entity2insert = new InvokeRecordDetailDO();
        entity2insert.setInvokeRecordId(invokeRecordId);
        entity2insert.setModelName(modelName);
        entity2insert.setCostToken(costToken);
        // 状态
        entity2insert.setStatus(status);
        entity2insert.setFailReason(failReason);
        Date endTime = new Date();
        entity2insert.setCostTime(Long.valueOf(endTime.getTime() - startTime.getTime()).intValue());
        entity2insert.setStartTime(startTime);
        entity2insert.setEndTime(endTime);
        entity2insert.setUserInput(userInput);
        entity2insert.setAssistantMessage(assistantMessage);
        return entity2insert;
    }
}
