package com.osh.ai.assistant.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.osh.ai.assistant.common.bean.dto.TokenDTO;
import com.osh.ai.assistant.common.bean.entity.ManagerDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.manager.CacheWrapper;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.common.util.JwtUtil;
import com.osh.ai.assistant.common.util.PageUtil;
import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.manager.bean.req.manager.*;
import com.osh.ai.assistant.manager.bean.vo.ManagerVO;
import com.osh.ai.assistant.manager.mapper.ManagerMapper;
import com.osh.ai.assistant.manager.service.ManagerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class ManagerServiceImpl extends ServiceImpl<ManagerMapper, ManagerDO> implements ManagerService {
    @Resource
    private CacheWrapper cacheWrapper;
    @Override
    public LoginResultVO login(LoginReq loginReq) {
        String existedCode = cacheWrapper.get(loginReq.getCaptchaId(),String.class);
        if (StrUtil.isBlank(existedCode)) {
            throw new BizEx("验证码不存在或已过期");
        }
        if (!loginReq.getCode().equalsIgnoreCase(existedCode)) {
            throw new BizEx("验证码不正确");
        }
        // 删除验证码
        cacheWrapper.delete(loginReq.getCaptchaId());
        LambdaQueryWrapper<ManagerDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ManagerDO::getUsername,loginReq.getUsername());
        ManagerDO one = getOne(lqw);
        if (one == null) {
            throw new BizEx("用户不存在");
        }
        if (!BCrypt.checkpw(loginReq.getPwd(),one.getPwd())) {
            throw new BizEx("密码不正确");
        }
        TokenDTO tokenDTO = ConvertUtil.convert(one,TokenDTO.class);
        String token = JwtUtil.getToken(tokenDTO);
        LoginResultVO vo = ConvertUtil.convert(tokenDTO, LoginResultVO.class);
        vo.setToken(token);
        // 更新登录时间
        one.setLastLoginTime(new Date());
        updateById(one);
        return vo;
    }

    @Override
    public ManagerVO queryById(Long id) {
        ManagerDO existedEntity = getById(id);
        return ConvertUtil.convert(existedEntity,ManagerVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyById(ManagerUpdateReq updateReq) {
        // 更新字段为null的问题
        LambdaUpdateWrapper<ManagerDO> luw = new LambdaUpdateWrapper<>();
        luw.set(ManagerDO::getRealName,updateReq.getRealName())
                .set(ManagerDO::getSex,updateReq.getSex())
                // 若avatarPath为null,用这种方式也能更新,即null不会被mp忽略
                .set(ManagerDO::getAvatarPath,updateReq.getAvatarPath())
                .eq(ManagerDO::getId,updateReq.getId());
        update(new ManagerDO(),luw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePwd(UpdatePwdReq updatePwdReq) {
        ManagerDO existedEntity = getById(updatePwdReq.getId());
        if (existedEntity == null) {
            throw new BizEx("用户不存在");
        }
        if (!BCrypt.checkpw(updatePwdReq.getOriginalPwd(),existedEntity.getPwd())) {
            throw new BizEx("原始密码不正确");
        }
        existedEntity.setPwd(BCrypt.hashpw(updatePwdReq.getNewPwd()));
        updateById(existedEntity);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(ManagerAddReq addReq) {
        ManagerDO entity = ConvertUtil.convert(addReq,ManagerDO.class);
        entity.setPwd(BCrypt.hashpw(entity.getPwd()));
        save(entity);
    }

    @Override
    public Result<List<ManagerVO>> queryPage(ManagerPageReq pageReq) {
        IPage<ManagerDO> iPage = PageUtil.buildPage(pageReq);
        LambdaQueryWrapper<ManagerDO> lqw = new LambdaQueryWrapper<>();
        // 拼接查询条件
        lqw.like(StrUtil.isNotBlank(pageReq.getUsername()),ManagerDO::getUsername,pageReq.getUsername());
        lqw.like(StrUtil.isNotBlank(pageReq.getRealName()),ManagerDO::getRealName,pageReq.getRealName());
        lqw.eq(Objects.nonNull(pageReq.getSex()),ManagerDO::getSex,pageReq.getSex());
        lqw.orderByDesc(ManagerDO::getId);
        IPage<ManagerDO> page = page(iPage, lqw);
        return ConvertUtil.convert(page,ManagerVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if (id.equals(UserContext.get().getId())) {
            throw new BizEx("自己不能删除自己");
        }
        removeById(id);
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
}
