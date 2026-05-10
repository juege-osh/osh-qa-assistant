package com.osh.ai.assistant.consumer.mapper;
import com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibPageReq;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库mapper类

 */
public interface KnowledgeLibMapper extends BaseMapper<KnowledgeLibDO> {

    Long queryCount(KnowledgeLibPageReq pageReq);

    List<KnowledgeLibVO> queryList(KnowledgeLibPageReq pageReq);

    List<KnowledgeLibDO> listUnUsed(@Param("userId") Long userId);
}
