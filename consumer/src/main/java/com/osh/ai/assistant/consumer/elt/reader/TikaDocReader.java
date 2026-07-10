package com.osh.ai.assistant.consumer.elt.reader;

import cn.hutool.core.collection.CollUtil;
import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.service.OssService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件通用读取类。
 * <p>
 * 支持两种存储模式：
 * - local: 直接从本地 staticDir 读取文件（原有行为）
 * - oss: 先尝试本地缓存，若不存在则从 OSS 下载到临时文件再读取
 */
@Slf4j
@Component
public class TikaDocReader implements DocReader {
    @Resource
    private UploadProperties uploadProperties;
    @Resource
    private OssService ossService;

    @Override
    public List<Document> read(String storePath) {
        File file = resolveFile(storePath);
        if (!file.isFile() || !file.exists()) {
            // 本地文件不存在，OSS 模式下文件只存在于远端对象存储，需要下载到临时文件。
            if (uploadProperties.isOssStorage()) {
                file = downloadFromOss(storePath);
            } else {
                throw new BizEx("文件不存在");
            }
        }
        FileSystemResource fileSystemResource = new FileSystemResource(file);
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(fileSystemResource);
        List<Document> documents = tikaDocumentReader.read();
        if (CollUtil.isEmpty(documents)) {
            throw new BizEx("无法读取文件");
        }
        return documents;
    }

    /**
     * 解析本地文件路径（local 模式使用）。
     */
    public File resolveFile(String storePath) {
        return Path.of(uploadProperties.getStaticDir(), storePath).normalize().toFile();
    }

    /**
     * 从 OSS 下载文件到系统临时目录，返回临时文件引用。
     * 临时文件由操作系统负责清理，无需手动删除。
     */
    private File downloadFromOss(String storePath) {
        try {
            String extension = FilenameUtils.getExtension(storePath);
            String prefix = "oss-doc-";
            String suffix = extension.isEmpty() ? ".tmp" : "." + extension;
            File tempFile = Files.createTempFile(prefix, suffix).toFile();
            ossService.downloadToFile(storePath, tempFile);
            log.debug("从 OSS 下载文件到临时目录: storePath={}, tempFile={}", storePath, tempFile.getAbsolutePath());
            return tempFile;
        } catch (IOException e) {
            log.error("创建临时文件失败", e);
            throw new BizEx("文件不存在");
        }
    }
}
