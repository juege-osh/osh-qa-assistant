package com.osh.ai.assistant.consumer.bean.req.app;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 绑定知识库入参类

 */
@Data
public class BindLibReq {
    /**
    * 主键
    */
    @NotNull
    private Long id;
    /**
     * 关联的知识库id
     */
    @NotNull
    private Long libId;
}
