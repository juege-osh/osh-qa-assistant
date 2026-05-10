package com.osh.ai.assistant.common.service;

import com.osh.ai.assistant.common.bean.req.uploadfile.UploadFileReq;
import com.osh.ai.assistant.common.bean.vo.UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 存储业务类
 *
 */
public interface StorageService {
    /**
     * @return 格式如: resources/type/20230523/123.jpg
     */
    UploadResultVO uploadFile(UploadFileReq uploadFileReq);

    String storeFile(String originalFilename, String module, byte[] bytes);


    void check(MultipartFile multipartFile);
}
