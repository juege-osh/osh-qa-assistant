package com.osh.ai.assistant.consumer.elt.reader;

import cn.hutool.core.collection.CollUtil;
import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.ex.BizEx;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * 文件通用读取类
 */
@Component
public class TikaDocReader implements DocReader {
    @Resource
    private UploadProperties uploadProperties;
    @Override
    public List<Document> read(String storePath) {
        String absPath = uploadProperties.getStaticDir() + storePath;
        File file = new File(absPath);
        if (!file.isFile() || !file.exists()) {
            throw new BizEx("文件不存在");
        }
        FileSystemResource fileSystemResource = new FileSystemResource(file);
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(fileSystemResource);
        List<Document> documents = tikaDocumentReader.read();
        if (CollUtil.isEmpty(documents)) {
            throw new BizEx("无法读取文件");
        }
        return documents;
    }
}
