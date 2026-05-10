package com.osh.ai.assistant.manager.service;
import com.osh.ai.assistant.common.bean.res.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.osh.ai.assistant.common.bean.entity.UploadFileDO;
import com.osh.ai.assistant.manager.bean.req.uploadfile.UploadFilePageReq;
import com.osh.ai.assistant.manager.bean.vo.UploadFileVO;
import java.util.List;
/**
  * 文件信息业务类
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
public interface UploadFileService extends IService<UploadFileDO> {

    /**
    * 分页查询
    */
    Result<List<UploadFileVO>> queryPage(UploadFilePageReq pageReq);
}
