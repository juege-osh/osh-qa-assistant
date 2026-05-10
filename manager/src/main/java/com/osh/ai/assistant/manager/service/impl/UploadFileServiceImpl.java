package com.osh.ai.assistant.manager.service.impl;

import com.osh.ai.assistant.common.bean.entity.UploadFileDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.manager.bean.req.uploadfile.UploadFilePageReq;
import com.osh.ai.assistant.manager.bean.vo.UploadFileVO;
import com.osh.ai.assistant.manager.mapper.UploadFileMapper;
import com.osh.ai.assistant.manager.service.UploadFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
/**
 * 文件信息业务实现类

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadFileServiceImpl extends ServiceImpl<UploadFileMapper,UploadFileDO> implements UploadFileService {

    @Override
    public Result<List<UploadFileVO>> queryPage(UploadFilePageReq pageReq) {
        Long count = getBaseMapper().queryCount(pageReq);
        if (count.equals(CommonConstants.ZERO_LONG)) {
            return Result.buildSuccess(Collections.emptyList(), count);
        }
        List<UploadFileVO> vos = getBaseMapper().queryList(pageReq);
        return Result.buildSuccess(vos, count);
    }
}
