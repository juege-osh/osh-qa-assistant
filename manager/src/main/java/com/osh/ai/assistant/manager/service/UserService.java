package com.osh.ai.assistant.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.manager.bean.req.user.UserPageReq;
import com.osh.ai.assistant.manager.bean.vo.UserVO;

import java.util.List;
/**
  * 用户表业务类
  */
public interface UserService extends IService<UserDO> {

    /**
    * 分页查询
    */
    Result<List<UserVO>> queryPage(UserPageReq pageReq);

    UserVO queryById(Long id);
}
