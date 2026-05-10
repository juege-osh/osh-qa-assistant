package com.osh.ai.assistant.manager.mapper;
import com.osh.ai.assistant.manager.bean.req.uploadfile.UploadFilePageReq;
import com.osh.ai.assistant.manager.bean.vo.UploadFileVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.osh.ai.assistant.common.bean.entity.UploadFileDO;

import java.util.List;

/**
 * 文件信息mapper类

 */
public interface UploadFileMapper extends BaseMapper<UploadFileDO> {

    Long queryCount(UploadFilePageReq pageReq);

    List<UploadFileVO> queryList(UploadFilePageReq pageReq);
}
