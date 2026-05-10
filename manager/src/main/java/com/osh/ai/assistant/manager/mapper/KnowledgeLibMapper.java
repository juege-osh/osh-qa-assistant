package com.osh.ai.assistant.manager.mapper;
import com.osh.ai.assistant.manager.bean.req.knowledgelib.KnowledgeLibPageReq;
import com.osh.ai.assistant.manager.bean.vo.KnowledgeLibVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO;

import java.util.List;

/**
 * 知识库mapper类

 */
public interface KnowledgeLibMapper extends BaseMapper<KnowledgeLibDO> {

    Long queryCount(KnowledgeLibPageReq pageReq);

    List<KnowledgeLibVO> queryList(KnowledgeLibPageReq pageReq);
}
