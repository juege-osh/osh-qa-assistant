package com.osh.ai.assistant.backend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.osh.ai.assistant.backend.bean.req.AuthLoginReq;
import com.osh.ai.assistant.backend.service.AuthService;
import com.osh.ai.assistant.common.bean.dto.TokenDTO;
import com.osh.ai.assistant.common.bean.entity.ManagerDO;
import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.enums.AccountRoleEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.manager.CacheWrapper;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.common.util.JwtUtil;
import com.osh.ai.assistant.consumer.mapper.UserMapper;
import com.osh.ai.assistant.manager.mapper.ManagerMapper;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {
    @Resource
    private CacheWrapper cacheWrapper;

    @Resource
    private ManagerMapper managerMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public CodeVO getCode() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(150, 40, 2);
        captcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        captcha.setFont(new Font("楷体", Font.BOLD, 28));
        String capText = captcha.text();
        String captchaId = UUID.randomUUID().toString();
        cacheWrapper.set(captchaId, capText, CommonConstants.CAPTCHA_EXPIRE_IN_MINUTES, TimeUnit.MINUTES);

        CodeVO codeVO = new CodeVO();
        codeVO.setCaptchaId(captchaId);
        codeVO.setText(captcha.toBase64());
        return codeVO;
    }

    @Override
    public LoginResultVO login(AuthLoginReq loginReq) {
        validateCode(loginReq.getCaptchaId(), loginReq.getCode());
        AccountRoleEnum role = AccountRoleEnum.of(loginReq.getRole());
        if (role == null) {
            throw new BizEx("账号角色不正确");
        }
        if (AccountRoleEnum.ADMIN.equals(role)) {
            return loginManager(loginReq);
        }
        return loginUser(loginReq);
    }

    private void validateCode(String captchaId, String code) {
        String existedCode = cacheWrapper.get(captchaId, String.class);
        if (StrUtil.isBlank(existedCode)) {
            throw new BizEx("验证码不存在或已过期");
        }
        if (!code.equalsIgnoreCase(existedCode)) {
            throw new BizEx("验证码不正确");
        }
        cacheWrapper.delete(captchaId);
    }

    private LoginResultVO loginManager(AuthLoginReq loginReq) {
        LambdaQueryWrapper<ManagerDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ManagerDO::getUsername, loginReq.getUsername());
        ManagerDO manager = managerMapper.selectOne(lqw);
        if (manager == null) {
            throw new BizEx("用户不存在");
        }
        if (!BCrypt.checkpw(loginReq.getPwd(), manager.getPwd())) {
            throw new BizEx("密码不正确");
        }

        TokenDTO tokenDTO = ConvertUtil.convert(manager, TokenDTO.class);
        tokenDTO.setRole(AccountRoleEnum.ADMIN.getCode());
        String token = JwtUtil.getToken(tokenDTO);
        LoginResultVO vo = ConvertUtil.convert(tokenDTO, LoginResultVO.class);
        vo.setToken(token);

        manager.setLastLoginTime(new Date());
        managerMapper.updateById(manager);
        return vo;
    }

    private LoginResultVO loginUser(AuthLoginReq loginReq) {
        LambdaQueryWrapper<UserDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(UserDO::getUsername, loginReq.getUsername());
        UserDO user = userMapper.selectOne(lqw);
        if (user == null) {
            throw new BizEx("用户不存在");
        }
        if (!BCrypt.checkpw(loginReq.getPwd(), user.getPwd())) {
            throw new BizEx("密码不正确");
        }

        TokenDTO tokenDTO = ConvertUtil.convert(user, TokenDTO.class);
        tokenDTO.setRole(AccountRoleEnum.USER.getCode());
        String token = JwtUtil.getToken(tokenDTO);
        LoginResultVO vo = ConvertUtil.convert(tokenDTO, LoginResultVO.class);
        vo.setToken(token);
        return vo;
    }
}
