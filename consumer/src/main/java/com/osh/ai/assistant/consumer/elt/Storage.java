package com.osh.ai.assistant.consumer.elt;

import com.osh.ai.assistant.consumer.bean.dto.StoreResultDTO;

/**
 * 存储接口
 */
public interface Storage {
    /**
     * 存储文件
     * @param storePath 文件的存储路径,格式如:resources/type/20230523/123.txt
     * @param libId 所属知识库id
     */
    StoreResultDTO store(String storePath,Long libId);

    /**
     * 从向量数据库中删除
     * @param docIds 文档id列表,json数组序列化后的字符串
     */
    void deleteByIds(String docIds);
}
