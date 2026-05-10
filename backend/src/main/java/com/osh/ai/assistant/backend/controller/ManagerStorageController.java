package com.osh.ai.assistant.backend.controller;

import com.osh.ai.assistant.common.bean.req.uploadfile.UploadFileReq;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.bean.vo.UploadResultVO;
import com.osh.ai.assistant.common.service.StorageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/storage")
@Validated
@Slf4j
public class ManagerStorageController {
    @Resource
    private StorageService storageService;

    @PostMapping("/uploadFile")
    public Result<UploadResultVO> uploadFile(@Validated UploadFileReq uploadFileReq) {
        return Result.buildSuccess(storageService.uploadFile(uploadFileReq));
    }
}
