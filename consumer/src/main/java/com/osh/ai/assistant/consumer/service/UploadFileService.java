package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.common.bean.entity.UploadFileDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.req.uploadfile.UploadFileAddReq;
import com.osh.ai.assistant.consumer.bean.req.uploadfile.UploadFilePageReq;
import com.osh.ai.assistant.consumer.bean.req.uploadfile.UploadFileUpdateStatusReq;
import com.osh.ai.assistant.consumer.bean.vo.UploadFileVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
  * 文件信息业务类
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
public interface UploadFileService extends IService<UploadFileDO> {
    /**
    * 新增
    */
    void add(UploadFileAddReq addReq);

    /**
    * 分页查询
    */
    Result<List<UploadFileVO>> queryPage(UploadFilePageReq pageReq);

    /**
     * 按主键删除
     */
    void deleteById(Long id);

    /**
    * 按id查询
    */
    UploadFileVO queryById(Long id);

    /**
     * 校验当前登录用户是否拥有该文件
     */
    UploadFileDO requireOwnedEntity(Long id);

    List<UploadFileDO> selectByLibId(Long libId);

    /**
     * 对召回次数+1
     * @param docIds
     */
    void incrRecallCount(List<String> docIds);

    void updateStatus(UploadFileUpdateStatusReq req);

    void deleteByLibId(Long libId);
}
