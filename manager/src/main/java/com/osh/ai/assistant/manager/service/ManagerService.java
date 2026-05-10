package com.osh.ai.assistant.manager.service;

import com.osh.ai.assistant.common.bean.entity.ManagerDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;
import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.manager.bean.req.manager.*;
import com.osh.ai.assistant.manager.bean.vo.ManagerVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


public interface ManagerService extends IService<ManagerDO> {
    /**
     * 登录
     */
    LoginResultVO login(LoginReq loginReq);

    /**
     * 根据主键查询
     */
    ManagerVO queryById(Long id);

    /**
     * 根据id修改信息
     */
    void modifyById(ManagerUpdateReq updateReq);
    /**
     * 根据id修改密码
     */
    void updatePwd(UpdatePwdReq updatePwdReq);
    /**
    * 新增
    */
    void add(ManagerAddReq addReq);
    /**
    * 分页查询
    */
    Result<List<ManagerVO>> queryPage(ManagerPageReq pageReq);
    /**
    * 按主键删除
    */
    void deleteById(Long id);

    CodeVO getCode();
}
