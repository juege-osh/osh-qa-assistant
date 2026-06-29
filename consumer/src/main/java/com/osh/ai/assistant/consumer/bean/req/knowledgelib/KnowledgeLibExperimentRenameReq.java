package com.osh.ai.assistant.consumer.bean.req.knowledgelib;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeLibExperimentRenameReq {
    @NotNull(message = "实验版本不能为空")
    private Long id;
    private String versionName;
}
