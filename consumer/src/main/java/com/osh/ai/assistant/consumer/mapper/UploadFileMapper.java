package com.osh.ai.assistant.consumer.mapper;

import com.osh.ai.assistant.common.bean.entity.UploadFileDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * 文件信息mapper类

 */
public interface UploadFileMapper extends BaseMapper<UploadFileDO> {
    @Select("select * from upload_file where JSON_CONTAINS(doc_ids, json_array(#{docId}))")
    UploadFileDO selectByDocId(String docId);
}
