package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.bean.entity.AppPublishConfigDO;
import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.enums.AppPublishAccessTypeEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.manager.CacheWrapper;
import com.osh.ai.assistant.consumer.bean.dto.ChatDTO;
import com.osh.ai.assistant.consumer.bean.dto.PublicAppAccessTokenDTO;
import com.osh.ai.assistant.consumer.bean.req.publicapp.PublicAppChatReq;
import com.osh.ai.assistant.consumer.bean.req.publicapp.PublicAppVerifyPasswordReq;
import com.osh.ai.assistant.consumer.bean.vo.PublicAppAccessVO;
import com.osh.ai.assistant.consumer.bean.vo.PublicAppDetailVO;
import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;
import com.osh.ai.assistant.consumer.manager.InvokeManager;
import com.osh.ai.assistant.consumer.service.AiChatService;
import com.osh.ai.assistant.consumer.service.AppPublishConfigService;
import com.osh.ai.assistant.consumer.service.AppService;
import com.osh.ai.assistant.consumer.service.PublicAppService;
import com.osh.ai.assistant.consumer.service.UserService;
import jakarta.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 公开应用访问业务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PublicAppServiceImpl implements PublicAppService {
    private static final String ACCESS_TOKEN_CACHE_PREFIX = "public-app:access-token:";
    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 12 * 60 * 60;

    @Resource
    private ExecutorService executorService;
    @Resource
    private AppPublishConfigService appPublishConfigService;
    @Resource
    private AppService appService;
    @Resource
    private UserService userService;
    @Resource
    private InvokeManager invokeManager;
    @Resource
    private AiChatService aiChatService;
    @Resource
    private CacheWrapper cacheWrapper;

    @Override
    public PublicAppDetailVO queryDetail(String slug) {
        PublishAccessContext context = requireEnabledContext(slug);
        PublicAppDetailVO vo = new PublicAppDetailVO();
        vo.setSlug(context.config().getSlug());
        vo.setAppName(context.app().getAppName());
        vo.setAppDesc(context.app().getAppDesc());
        vo.setIconPath(context.app().getIconPath());
        vo.setAccessType(context.config().getAccessType());
        vo.setPasswordRequired(AppPublishAccessTypeEnum.PASSWORD.getCode().equals(context.config().getAccessType()));
        return vo;
    }

    @Override
    public PublicAppAccessVO verifyPassword(PublicAppVerifyPasswordReq req) {
        PublishAccessContext context = requireEnabledContext(req.getSlug());
        AppPublishConfigDO config = context.config();
        if (!AppPublishAccessTypeEnum.PASSWORD.getCode().equals(config.getAccessType())) {
            throw new BizEx("当前公开应用无需访问密码");
        }
        if (StrUtil.isBlank(config.getPasswordHash())
            || !BCrypt.checkpw(req.getAccessPassword(), config.getPasswordHash())) {
            throw new BizEx("访问密码错误");
        }
        String accessToken = IdUtil.fastSimpleUUID();
        PublicAppAccessTokenDTO tokenDTO = new PublicAppAccessTokenDTO();
        tokenDTO.setSlug(config.getSlug());
        tokenDTO.setPasswordHash(config.getPasswordHash());
        cacheWrapper.set(buildAccessTokenCacheKey(accessToken), tokenDTO, ACCESS_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        PublicAppAccessVO vo = new PublicAppAccessVO();
        vo.setAccessToken(accessToken);
        vo.setExpireSeconds(ACCESS_TOKEN_EXPIRE_SECONDS);
        return vo;
    }

    @Override
    public SseEmitter chat(PublicAppChatReq req) {
        PublishAccessContext context = requireEnabledContext(req.getSlug());
        validatePasswordAccess(context.config(), req.getAccessToken());
        if (context.app().getLibId() == null && !Objects.equals(context.app().getOutLibEnable(), 1)) {
            throw new BizEx("公开应用暂未配置可用问答能力");
        }
        SseEmitter emitter = new SseEmitter(0L);
        ChatDTO chatDTO = new ChatDTO();
        chatDTO.setUserInput(req.getUserInput());
        chatDTO.setConversationId(context.config().getSlug() + CommonConstants.UNDER_LINE + req.getVisitorId());
        chatDTO.setAppKey(context.owner().getAppKey());
        InvokeRecordBuilder builder = invokeManager.initInvokeRecordBuild(
            chatDTO,
            buildPublicInvokeUser(context.owner(), req.getVisitorId()),
            context.app()
        );
        executorService.execute(() -> aiChatService.doChat(emitter, context.app(), builder, true));
        return emitter;
    }

    private PublishAccessContext requireEnabledContext(String slug) {
        AppPublishConfigDO config = appPublishConfigService.requireEnabledBySlug(slug);
        AppDO app = appService.getById(config.getAppId());
        if (app == null) {
            throw new BizEx("公开应用不存在或已关闭");
        }
        UserDO owner = userService.getById(app.getUserId());
        if (owner == null || StrUtil.isBlank(owner.getAppKey())) {
            throw new BizEx("公开应用所属账号不可用");
        }
        return new PublishAccessContext(config, app, owner);
    }

    private void validatePasswordAccess(AppPublishConfigDO config, String accessToken) {
        if (!AppPublishAccessTypeEnum.PASSWORD.getCode().equals(config.getAccessType())) {
            return;
        }
        if (StrUtil.isBlank(accessToken)) {
            throw new BizEx("请先完成访问密码验证");
        }
        PublicAppAccessTokenDTO tokenDTO = cacheWrapper.get(buildAccessTokenCacheKey(accessToken), PublicAppAccessTokenDTO.class);
        if (tokenDTO == null
            || !StrUtil.equals(config.getSlug(), tokenDTO.getSlug())
            || !StrUtil.equals(config.getPasswordHash(), tokenDTO.getPasswordHash())) {
            throw new BizEx("访问凭证已失效，请重新验证访问密码");
        }
    }

    private String buildAccessTokenCacheKey(String accessToken) {
        return ACCESS_TOKEN_CACHE_PREFIX + accessToken;
    }

    private UserDO buildPublicInvokeUser(UserDO owner, String visitorId) {
        UserDO invokeUser = new UserDO();
        invokeUser.setId(owner.getId());
        invokeUser.setAppKey(owner.getAppKey());
        invokeUser.setUsername(buildPublicVisitorUsername(owner.getUsername(), visitorId));
        return invokeUser;
    }

    private String buildPublicVisitorUsername(String ownerUsername, String visitorId) {
        String ownerPart = StrUtil.blankToDefault(StrUtil.trim(ownerUsername), "owner");
        String visitorPart = StrUtil.blankToDefault(StrUtil.trim(visitorId), "guest");
        if (visitorPart.length() > 8) {
            visitorPart = visitorPart.substring(0, 8);
        }
        return ownerPart + " / 公开访客:" + visitorPart;
    }

    private record PublishAccessContext(AppPublishConfigDO config, AppDO app, UserDO owner) {
    }
}
