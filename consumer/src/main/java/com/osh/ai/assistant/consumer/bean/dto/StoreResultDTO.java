package com.osh.ai.assistant.consumer.bean.dto;

import lombok.Data;

import java.util.List;


@Data
public class StoreResultDTO {
    /**
     * 文件字符数
     */
    private Long charCount;
    /**
     * 关联的文档id列表
     */
    private List<String> docIds;
}
