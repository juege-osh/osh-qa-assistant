package com.osh.ai.assistant.common.service.impl;

import cn.hutool.core.date.DateUtil;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.enums.UploadPathEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.service.OssService;
import com.osh.ai.assistant.common.util.OssUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * S3 兼容 OSS 服务实现
 */
@Service
public class OssServiceImpl implements OssService {

    @Resource
    private OssUtil ossUtil;

    @Override
    public String upload(MultipartFile file, UploadPathEnum pathEnum, String bizId) {
        try {
            return upload(file.getOriginalFilename(), file.getContentType(), file.getBytes(), pathEnum, bizId);
        } catch (Exception e) {
            throw new BizEx("上传OSS文件失败");
        }
    }

    @Override
    public String upload(String originalFilename, String contentType, byte[] bytes, UploadPathEnum pathEnum, String bizId) {
        UploadPathEnum uploadPathEnum = pathEnum == null ? UploadPathEnum.RESOURCE : pathEnum;
        String customPath = buildCustomPath(uploadPathEnum, bizId);
        return ossUtil.uploadFile(originalFilename, contentType, bytes, customPath);
    }

    @Override
    public String getLimitedUrl(String fileKey, int minute) {
        if (StringUtils.isBlank(fileKey)) {
            throw new BizEx("文件路径不能为空");
        }
        return ossUtil.getSignedUrl(StringUtils.removeStart(fileKey, CommonConstants.SLASH), minute);
    }

    @Override
    public boolean existsFileKey(String fileKey) {
        return ossUtil.existsFileKey(fileKey);
    }

    @Override
    public boolean deleteFile(String fileKey) {
        return ossUtil.deleteFile(fileKey);
    }

    private String buildCustomPath(UploadPathEnum pathEnum, String bizId) {
        String patternDate = DateUtil.format(LocalDateTime.now(), CommonConstants.UPLOAD_DATE_FORMAT);
        String normalizedBizId = StringUtils.isBlank(bizId) ? "" : StringUtils.strip(bizId, CommonConstants.SLASH) + CommonConstants.SLASH;
        return pathEnum.getPath() + normalizedBizId + patternDate;
    }
}
