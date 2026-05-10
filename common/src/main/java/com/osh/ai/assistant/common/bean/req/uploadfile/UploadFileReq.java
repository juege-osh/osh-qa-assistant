package com.osh.ai.assistant.common.bean.req.uploadfile;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;



@Data
public class UploadFileReq {
    @NotNull
    private MultipartFile file;
    /**
     * 指定上传的子目录 /resources/ [module/] 20240601/ uuid.jpg
     */
    private String module;
}
