package com.osh.ai.assistant.consumer.bean.req.uploadfile;

import com.osh.ai.assistant.common.bean.req.BasePageReq;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件信息分页查询入参
 *

 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UploadFilePageReq extends BasePageReq {

    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 知识库id
     */
    @NotNull
    private Long libId;
}
