package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.osh.ai.assistant.common.bean.dto.TokenDTO;
import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.common.enums.AccountRoleEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.manager.CacheWrapper;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.common.util.JwtUtil;
import com.osh.ai.assistant.consumer.bean.req.user.LoginReq;
import com.osh.ai.assistant.consumer.bean.req.user.UpdatePwdReq;
import com.osh.ai.assistant.consumer.bean.req.user.UserRegisterReq;
import com.osh.ai.assistant.consumer.bean.req.user.UserUpdateReq;
import com.osh.ai.assistant.consumer.bean.vo.UserVO;
import com.osh.ai.assistant.consumer.mapper.UserMapper;
import com.osh.ai.assistant.consumer.service.UserService;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户表业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    @Resource
    private CacheWrapper cacheWrapper;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterReq registerReq) {
        validateCode(registerReq.getCaptchaId(),registerReq.getCode());
        UserDO entity = ConvertUtil.convert(registerReq,UserDO.class);
        entity.setPwd(BCrypt.hashpw(entity.getPwd()));
        entity.setAppKey(IdUtil.fastSimpleUUID());
        entity.setRegisterTime(new Date());
        save(entity);
    }

    /**
     * 校验验证码
     */
    private void validateCode(String captchaId, String code) {
        String existedCode = cacheWrapper.get(captchaId,String.class);
        if (StrUtil.isBlank(existedCode)) {
            throw new BizEx("验证码不存在或已过期");
        }
        if (!code.equalsIgnoreCase(existedCode)) {
            throw new BizEx("验证码不正确");
        }
        // 删除验证码
        cacheWrapper.delete(captchaId);
    }

    @Override
    public UserVO queryById(Long id) {
        UserDO entity = requireCurrentUser(id);
        UserVO vo = ConvertUtil.convert(entity,UserVO.class);
        return vo;
    }

    @Override
    public LoginResultVO login(LoginReq loginReq) {
        validateCode(loginReq.getCaptchaId(),loginReq.getCode());
        LambdaQueryWrapper<UserDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(UserDO::getUsername,loginReq.getUsername());
        UserDO one = getOne(lqw);
        if (one == null) {
            throw new BizEx("用户不存在");
        }
        if (!BCrypt.checkpw(loginReq.getPwd(),one.getPwd())) {
            throw new BizEx("密码不正确");
        }
        TokenDTO tokenDTO = ConvertUtil.convert(one,TokenDTO.class);
        tokenDTO.setRole(AccountRoleEnum.USER.getCode());
        String token = JwtUtil.getToken(tokenDTO);
        LoginResultVO vo = ConvertUtil.convert(tokenDTO, LoginResultVO.class);
        vo.setToken(token);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePwd(UpdatePwdReq updatePwdReq) {
        UserDO existedEntity = requireCurrentUser(updatePwdReq.getId());
        if (!BCrypt.checkpw(updatePwdReq.getOriginalPwd(),existedEntity.getPwd())) {
            throw new BizEx("原始密码不正确");
        }
        existedEntity.setPwd(BCrypt.hashpw(updatePwdReq.getNewPwd()));
        updateById(existedEntity);
    }

    @Override
    public UserDO selectByAppKey(String appKey) {
        LambdaQueryWrapper<UserDO> lqw = Wrappers.<UserDO>lambdaQuery().eq(UserDO::getAppKey, appKey);
        return getOne(lqw);
    }

    @Override
    public UserDO requireCurrentUser(Long id) {
        UserDO entity = getById(id);
        if (entity == null || !id.equals(UserContext.getUserId())) {
            throw new BizEx("用户不存在或无权限访问");
        }
        return entity;
    }

    @Override
    public CodeVO getCode() {
        // 两位运算,其他如 ChineseCaptcha等
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(150, 40, 2);
        // 纯数字
        captcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        captcha.setFont(new Font("楷体", Font.BOLD, 28));
        // 运算结果
        String capText = captcha.text();
        String captchaId  = UUID.randomUUID().toString();
        //存储验证码
        cacheWrapper.set(captchaId, capText, CommonConstants.CAPTCHA_EXPIRE_IN_MINUTES, TimeUnit.MINUTES);
        CodeVO codeVO = new CodeVO();
        codeVO.setCaptchaId(captchaId);
        // 验证码图片
        codeVO.setText(captcha.toBase64());
        return codeVO;
    }

    @Override
    public void modifyById(UserUpdateReq updateReq) {
        requireCurrentUser(updateReq.getId());
        // 更新字段为null的问题
        LambdaUpdateWrapper<UserDO> luw = new LambdaUpdateWrapper<>();
        luw
            // 若avatarPath为null,用这种方式也能更新,即null不会被mp忽略
            .set(UserDO::getAvatarPath,updateReq.getAvatarPath())
            .eq(UserDO::getId,updateReq.getId());
        update(new UserDO(),luw);
    }
}
