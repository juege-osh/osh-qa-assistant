package com.osh.ai.assistant.consumer.bean.req.ops;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AlertSelfCheckReq {
    /**
     * 演练类型：database / qdrant / generic
     */
    @NotBlank(message = "演练类型不能为空")
    private String scene;
    /**
     * 本次演练备注
     */
    private String note;
}
