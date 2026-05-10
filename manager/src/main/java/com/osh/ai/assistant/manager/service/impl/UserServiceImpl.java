package com.osh.ai.assistant.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.common.util.PageUtil;
import com.osh.ai.assistant.manager.bean.req.user.UserPageReq;
import com.osh.ai.assistant.manager.bean.vo.UserVO;
import com.osh.ai.assistant.manager.mapper.UserMapper;
import com.osh.ai.assistant.manager.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * 用户表业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    @Override
    public Result<List<UserVO>> queryPage(UserPageReq pageReq) {
        IPage<UserDO> iPage = PageUtil.buildPage(pageReq);
        LambdaQueryWrapper<UserDO> lqw = new LambdaQueryWrapper<>();
        // 拼接查询条件
        lqw.like(StrUtil.isNotBlank(pageReq.getUsername()),UserDO::getUsername,pageReq.getUsername());
        lqw.orderByDesc(UserDO::getId);
        IPage<UserDO> page = page(iPage, lqw);
        return ConvertUtil.convert(page,UserVO.class);
    }
    @Override
    public UserVO queryById(Long id) {
        UserDO existedEntity = getById(id);
        return ConvertUtil.convert(existedEntity, UserVO.class);
    }
}
