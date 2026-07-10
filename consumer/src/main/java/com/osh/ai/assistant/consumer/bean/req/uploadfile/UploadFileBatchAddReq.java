package com.osh.ai.assistant.consumer.bean.req.uploadfile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UploadFileBatchAddReq {

    @NotEmpty
    @Size(max = 10)
    @Valid
    private List<UploadFileAddReq> files;
}
