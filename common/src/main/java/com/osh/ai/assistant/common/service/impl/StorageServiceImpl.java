package com.osh.ai.assistant.common.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import com.osh.ai.assistant.common.bean.req.uploadfile.UploadFileReq;
import com.osh.ai.assistant.common.bean.vo.UploadResultVO;
import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.enums.UploadPathEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.service.OssService;
import com.osh.ai.assistant.common.service.StorageService;
import com.osh.ai.assistant.common.util.PathUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * 存储业务实现类
 *
 */
@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    @Resource
    private UploadProperties uploadProperties;
    @Resource
    private OssService ossService;

    @Override
    public UploadResultVO uploadFile(UploadFileReq uploadFileReq) {
        MultipartFile multipartFile = uploadFileReq.getFile();
        check(multipartFile);
        String originalFilename = multipartFile.getOriginalFilename();
        String module = uploadFileReq.getModule();
        byte[] bytes;
        try {
            bytes = multipartFile.getBytes();
        }catch (Exception e) {
            log.error("获取文件内容出错",e);
            throw new BizEx("获取文件内容出错");
        }
        // 保持 local 为默认行为，仅在 upload.storage-type=oss 时切换到远端对象存储。
        String relativePath = uploadProperties.isOssStorage()
                ? storeOssFile(originalFilename, multipartFile.getContentType(), module, bytes)
                : storeFile(originalFilename,module,bytes);
        // 设置返回结果
        UploadResultVO vo = new UploadResultVO();
        vo.setOriginalFilename(originalFilename);
        vo.setRelativePath(relativePath);
        vo.setSize(multipartFile.getSize());
        vo.setStorageType(uploadProperties.isOssStorage() ? "oss" : "local");
        if (uploadProperties.isOssStorage()) {
            // OSS 模式下 relativePath 本身就是对象 key，额外返回便于调用方删除或生成签名 URL。
            vo.setObjectKey(relativePath);
            vo.setUrl(buildOssUrl(relativePath));
        }
        return vo;
    }

    /**
     * 存储文件
     * @param originalFilename 原始文件名
     * @param bytes 文件内容
     * @param module 模块
     * @return 存储的相对路径
     */
    @Override
    public String storeFile(String originalFilename, String module, byte[] bytes) {
        if (StringUtils.isBlank(originalFilename)) {
            log.error("上传的文件原始名称为空");
            throw new BizEx("上传的文件原始名称为空");
        }
        if (ArrayUtil.isEmpty(bytes)) {
            log.error("文件内容为空");
            throw new BizEx("文件内容为空");
        }
        String relativePath = spliceRelativePath4File(originalFilename, module);
        // 进行文件存储
        doStoreFile(relativePath, bytes);
        return relativePath;
    }

    @Override
    public void check(MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            log.warn("未上传或上传的文件内容为空");
            throw new BizEx("未上传或上传的文件内容为空");
        }
        String originalFilename = multipartFile.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            log.warn("上传的文件原始名称为空");
            throw new BizEx("上传的文件原始名称为空");
        }
    }

    /**
     * 进行文件存储
     * @param relativePath  非分片:resources/ type/ 20230523/ 123.jpg
     *                      分片: temp/requestId/0_xxx.mp4
     */
    private void doStoreFile(String relativePath, byte[] bytes) {
        File destFile = Path.of(uploadProperties.getStaticDir(), relativePath).normalize().toFile();
        PathUtil.createDir(destFile.getParentFile());
        try {
            FileUtil.writeBytes(bytes,destFile);
        } catch (Exception e) {
            log.error("store image or file error,filePath:{}", destFile.getAbsolutePath(), e);
            throw new BizEx("存储文件出错");
        }
    }

    private String storeOssFile(String originalFilename, String contentType, String module, byte[] bytes) {
        // module 只允许映射到预定义目录，避免用户输入直接污染 OSS key 前缀。
        UploadPathEnum uploadPathEnum = UploadPathEnum.fromModule(module);
        return ossService.upload(originalFilename, contentType, bytes, uploadPathEnum, null);
    }

    private String buildOssUrl(String objectKey) {
        UploadProperties.Oss oss = uploadProperties.getOss();
        String publicDomain = oss == null ? "" : oss.getPublicDomain();
        if (StringUtils.isBlank(publicDomain)) {
            return null;
        }
        return StringUtils.removeEnd(publicDomain, CommonConstants.SLASH)
                + CommonConstants.SLASH
                + StringUtils.removeStart(objectKey, CommonConstants.SLASH);
    }

    /**
     * 拼接上传文件在本地存储的相对路径,格式如: resources/ type/ 20230523/ 123.jpg
     *
     * @param originalFilename 原始文件名
     */
    private String spliceRelativePath4File(String originalFilename, String module) {
        String patternDate = DateUtil.format(LocalDateTime.now(), CommonConstants.UPLOAD_DATE_FORMAT);
        String baseName = IdUtil.fastSimpleUUID();
        String suffix = CommonConstants.DOT + FilenameUtils.getExtension(originalFilename);
        //123.jpg
        String newFileName = baseName + suffix;
        //relativePath: resources/ type/ 20230523/ 123.jpg
        StringBuilder relativePath = new StringBuilder();
        relativePath.append(CommonConstants.RESOURCES_PREFIX);
        // 校验模块名是否为空
        if (StringUtils.isNoneBlank(module)) {
            relativePath.append(module).append(CommonConstants.SLASH);
        }
        relativePath.append(patternDate).append(newFileName);
        return relativePath.toString();
    }
}
