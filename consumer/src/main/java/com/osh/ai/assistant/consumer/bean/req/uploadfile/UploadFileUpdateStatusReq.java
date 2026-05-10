package com.osh.ai.assistant.consumer.bean.req.uploadfile;

import com.osh.ai.assistant.common.enums.UploadFileStatusEnum;
import com.osh.ai.assistant.common.validator.EnumValue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改上传文件状态入参
 *

 */
@Data
public class UploadFileUpdateStatusReq {
    /**
     * 主键
     */
    @NotNull
    private Long id;
    /**
     * 知识库id
     */
    @NotNull
    @EnumValue(clazz = UploadFileStatusEnum.class)
    private Integer status;
}
