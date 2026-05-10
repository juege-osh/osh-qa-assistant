package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.req.uploadfile.UploadFileAddReq;
import com.osh.ai.assistant.consumer.bean.req.uploadfile.UploadFilePageReq;
import com.osh.ai.assistant.consumer.bean.req.uploadfile.UploadFileUpdateStatusReq;
import com.osh.ai.assistant.consumer.bean.vo.UploadFileVO;
import com.osh.ai.assistant.consumer.service.UploadFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
  * 文件信息Controller层
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
@RestController
@RequestMapping("/uploadFile")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UploadFileController {

    private final UploadFileService uploadFileService;

    /**
     * 文件信息新增
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Validated UploadFileAddReq addReq) {
        uploadFileService.add(addReq);
        return Result.buildSuccessMsg("添加成功");
    }

    /**
     * 文件信息分页查询
     */
    @PostMapping("/queryPage")
    public Result<List<UploadFileVO>> queryPage(@RequestBody @Validated UploadFilePageReq pageReq) {
        return  uploadFileService.queryPage(pageReq);
    }

    /**
     * 按id删除文件信息
     */
    @GetMapping("/deleteById")
    public Result<Void> deleteById(@RequestParam("id") Long id) {
        uploadFileService.deleteById(id);
        return Result.buildSuccessMsg("删除成功");
    }
    /**
     * 更新文件状态
     */
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@RequestBody @Validated UploadFileUpdateStatusReq req) {
        uploadFileService.updateStatus(req);
        return Result.buildSuccessMsg("状态修改成功");
    }
}
