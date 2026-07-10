package com.osh.ai.assistant.consumer.bean.req.uploadfile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FileSplitPreviewReq {
    /**
     * 文件 id
     */
    @NotNull(message = "文件不能为空")
    private Long id;
    /**
     * 试算切分策略
     */
    private String strategy;
    /**
     * 目标 chunk token 大小
     */
    @Min(value = 100, message = "chunk 大小不能小于 100")
    @Max(value = 4000, message = "chunk 大小不能大于 4000")
    private Integer chunkSize;
    /**
     * 最小 chunk 大小
     */
    @Min(value = 50, message = "最小 chunk 不能小于 50")
    @Max(value = 2000, message = "最小 chunk 不能大于 2000")
    private Integer minChunkSizeChars;
    /**
     * 最小保留长度
     */
    @Min(value = 1, message = "最小保留长度不能小于 1")
    @Max(value = 200, message = "最小保留长度不能大于 200")
    private Integer minChunkLengthToEmbed;
    /**
     * 最大 chunk 数
     */
    @Min(value = 1, message = "最大 chunk 数不能小于 1")
    @Max(value = 50000, message = "最大 chunk 数不能大于 50000")
    private Integer maxNumChunks;
    /**
     * 是否保留 separator
     */
    private Boolean keepSeparator;
    /**
     * 语义段长度上限
     */
    @Min(value = 100, message = "语义段长度上限不能小于 100")
    @Max(value = 5000, message = "语义段长度上限不能大于 5000")
    private Integer semanticSectionMaxChars;
    /**
     * 预览 chunk 数
     */
    @Min(value = 1, message = "预览 chunk 数不能小于 1")
    @Max(value = 30, message = "预览 chunk 数不能大于 30")
    private Integer previewChunkLimit;
}
