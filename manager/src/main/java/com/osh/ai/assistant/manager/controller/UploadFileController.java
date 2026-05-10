package com.osh.ai.assistant.manager.controller;
import com.osh.ai.assistant.common.bean.res.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.osh.ai.assistant.manager.bean.req.uploadfile.UploadFilePageReq;
import com.osh.ai.assistant.manager.bean.vo.UploadFileVO;
import com.osh.ai.assistant.manager.service.UploadFileService;
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
     * 文件信息分页查询
     */
    @PostMapping("/queryPage")
    public Result<List<UploadFileVO>> queryPage(@RequestBody UploadFilePageReq pageReq) {
        return  uploadFileService.queryPage(pageReq);
    }
}
