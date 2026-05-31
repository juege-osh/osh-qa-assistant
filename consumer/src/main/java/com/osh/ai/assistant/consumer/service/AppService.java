package com.osh.ai.assistant.consumer.service;
import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.consumer.bean.req.app.AppPageReq;
import com.osh.ai.assistant.consumer.bean.req.app.AppAddReq;
import com.osh.ai.assistant.consumer.bean.req.app.AppUpdateReq;
import com.osh.ai.assistant.consumer.bean.req.app.BindLibReq;
import com.osh.ai.assistant.consumer.bean.vo.AppVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
/**
  * 应用信息业务类
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
public interface AppService extends IService<AppDO> {
    /**
    * 新增
    */
    void add(AppAddReq addReq);

    /**
    * 分页查询
    */
    Result<List<AppVO>> queryPage(AppPageReq pageReq);

    /**
     * 按主键删除
     */
    void deleteById(Long id);

    /**
    * 按id查询
    */
    AppVO queryById(Long id);

    /**
    * 按id更新
    */
    void modifyById(AppUpdateReq updateReq);

    void checkChatCondition(Long id);
    /**
     * 解绑知识库
     */
    void unBindLib(Long id);

    void bindLib(BindLibReq req);

    /**
     * 校验当前登录用户是否拥有该应用
     */
    AppDO requireOwnedEntity(Long id);

    /**
     * 判断知识库是否已被绑定
     * @param libId 知识库id
     */
    boolean hasBound(Long libId);
}
