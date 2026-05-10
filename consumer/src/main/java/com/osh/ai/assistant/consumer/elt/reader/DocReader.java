package com.osh.ai.assistant.consumer.elt.reader;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文件读取
 */
public interface DocReader {
    /**
     * 读取文件
     * @param storePath 文件的存储路径,格式如:resources/type/20230523/123.txt
     */
    List<Document> read(String storePath);
}
