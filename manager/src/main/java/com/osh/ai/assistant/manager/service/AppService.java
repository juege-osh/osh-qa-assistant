package com.osh.ai.assistant.manager.service;
import com.osh.ai.assistant.common.bean.res.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.manager.bean.req.app.AppPageReq;
import com.osh.ai.assistant.manager.bean.vo.AppVO;
import java.util.List;
/**
  * 应用信息业务类
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
public interface AppService extends IService<AppDO> {
    /**
    * 分页查询
    */
    Result<List<AppVO>> queryPage(AppPageReq pageReq);
}
