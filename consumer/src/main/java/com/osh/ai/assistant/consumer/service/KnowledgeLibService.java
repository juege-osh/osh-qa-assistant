package com.osh.ai.assistant.consumer.service;
import com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibPageReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibAddReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibUpdateReq;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibVO;
import java.util.List;
/**
  * 知识库业务类
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
public interface KnowledgeLibService extends IService<KnowledgeLibDO> {
    /**
    * 新增
    */
    void add(KnowledgeLibAddReq addReq);

    /**
    * 分页查询
    */
    Result<List<KnowledgeLibVO>> queryPage(KnowledgeLibPageReq pageReq);

    /**
     * 按主键删除
     */
    void deleteById(Long id);

    /**
    * 按id查询
    */
    KnowledgeLibVO queryById(Long id);

    /**
     * 校验当前登录用户是否拥有该知识库
     */
    KnowledgeLibDO requireOwnedEntity(Long id);

    /**
    * 按id更新
    */
    void modifyById(KnowledgeLibUpdateReq updateReq);

    /**
     * 列出当前用户可用的知识库
     */
    List<KnowledgeLibVO> listAvailableLib(Long appId);
}
