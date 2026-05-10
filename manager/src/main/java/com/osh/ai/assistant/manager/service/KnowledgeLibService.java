package com.osh.ai.assistant.manager.service;
import com.osh.ai.assistant.common.bean.res.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO;
import com.osh.ai.assistant.manager.bean.req.knowledgelib.KnowledgeLibPageReq;
import com.osh.ai.assistant.manager.bean.vo.KnowledgeLibVO;
import java.util.List;
/**
  * 知识库业务类
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
public interface KnowledgeLibService extends IService<KnowledgeLibDO> {

    /**
    * 分页查询
    */
    Result<List<KnowledgeLibVO>> queryPage(KnowledgeLibPageReq pageReq);
}
