package com.osh.ai.assistant.consumer.builder;

import com.osh.ai.assistant.common.bean.entity.InvokeRecordDO;
import com.osh.ai.assistant.common.bean.entity.InvokeRecordDetailDO;
import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.common.enums.InvokeStatusEnum;
import com.osh.ai.assistant.consumer.bean.dto.ChatDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;


@Accessors(chain = true)
@Data
public class InvokeRecordBuilder {
    /**
     * id
     */
    private Long id;
    /**
     * 应用id
     */
    private Long appId;
    /**
     * 知识库id
     */
    private Long libId;
    /**
     * 一次调用的开始时间
     */
    private Date startTime;
    /**
     * 请求信息
     */
    private ChatDTO chatDto;
    /**
     * 终端消费人
     */
    private UserDO endUser;
    /**
     * 明细
     */
    private List<InvokeRecordDetailDO> detailList;

    private InvokeRecordBuilder(){}
    public static InvokeRecordBuilder builder() {
        return new InvokeRecordBuilder();
    }

    public InvokeRecordDO build() {
        InvokeRecordDO entity2insert = new InvokeRecordDO();
        entity2insert.setId(id);
        // 应用信息
        entity2insert.setAppId(appId);
        entity2insert.setLibId(libId);
        // 请求id
        entity2insert.setRequestId(chatDto.getRequestId());
        // 终端消费者信息
        entity2insert.setUserId(endUser.getId());
        entity2insert.setAppKey(endUser.getAppKey());
        entity2insert.setUsername(endUser.getUsername());
        // 状态
        entity2insert.setStatus(calcInvokeRecordStatus());
        Date endTime = new Date();
        entity2insert.setCostTime(Long.valueOf(endTime.getTime() - startTime.getTime()).intValue());
        entity2insert.setStartTime(startTime);
        entity2insert.setEndTime(endTime);
        return entity2insert;
    }

    private Integer calcInvokeRecordStatus() {
        boolean allMatch = detailList.stream()
            .allMatch(detail -> InvokeStatusEnum.SUCCESS.getCode().equals(detail.getStatus()));
        return  allMatch ? InvokeStatusEnum.SUCCESS.getCode() : InvokeStatusEnum.FAIL.getCode();
    }
}
