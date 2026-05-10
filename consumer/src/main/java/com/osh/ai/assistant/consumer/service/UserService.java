package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;
import com.osh.ai.assistant.consumer.bean.req.user.LoginReq;
import com.osh.ai.assistant.consumer.bean.req.user.UpdatePwdReq;
import com.osh.ai.assistant.consumer.bean.req.user.UserRegisterReq;
import com.osh.ai.assistant.consumer.bean.req.user.UserUpdateReq;
import com.osh.ai.assistant.consumer.bean.vo.UserVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
  * 用户表业务类
  */
public interface UserService extends IService<UserDO> {
    /**
    * 注册
    */
    void register(UserRegisterReq registerReq);

    /**
    * 按id查询
    */
    UserVO queryById(Long id);

    /**
     * 根据id修改密码
     */
    void updatePwd(UpdatePwdReq updatePwdReq);
    /**
     * 登录
     */
    LoginResultVO login(LoginReq loginReq);

    /**
     * 通过appKey查询用户
     */
    UserDO selectByAppKey(String appKey);

    CodeVO getCode();

    void modifyById(UserUpdateReq updateReq);
}
