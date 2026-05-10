package com.osh.ai.assistant.common.controller;

import com.osh.ai.assistant.common.bean.req.uploadfile.UploadFileReq;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.bean.vo.UploadResultVO;
import com.osh.ai.assistant.common.service.StorageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 存储相关接口
 */
@RestController
@RequestMapping("/storage")
@Validated
@Slf4j
public class StorageController {
    @Resource
    private StorageService storageService;

    @PostMapping("/uploadFile")
    public Result<UploadResultVO> uploadFile(@Validated UploadFileReq uploadFileReq) {
        return Result.buildSuccess(storageService.uploadFile(uploadFileReq));
    }
}
