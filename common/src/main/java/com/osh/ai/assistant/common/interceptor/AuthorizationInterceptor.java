package com.osh.ai.assistant.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.osh.ai.assistant.common.bean.dto.TokenDTO;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.common.enums.CodeEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;


@Slf4j
public class AuthorizationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // /manager/queryById
        log.info("请求路径:{}",request.getServletPath());
        String method = request.getMethod();
        /*
         * 简单请求:
         * 1.只限于get、post、head请求方式
         * 2.请求头不超出以下范围
         * Accept
         * Accept-Language
         * Content-Language
         * Last-Event-ID
         * Content-Type:application/x-www-form-urlencoded、multipart/form-data、text/plain
         * 非简单请求:除简单请求之外的
         * 客户端会对非简单请求发起OPTIONS请求用来判断服务端是否支持跨域
         */
        if (HttpMethod.OPTIONS.matches(method)) {
            // 去执行剩余的拦截器
            return true;
        }
        // 获取token
        String tokenToVerify = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isBlank(tokenToVerify)) {
            throw new BizEx(CodeEnum.AUTH_ERR);
        }
        // 校验token
        DecodedJWT decodedJWT = JwtUtil.verify(tokenToVerify);
        // 解析出结果
        TokenDTO tokenDTO = JwtUtil.parse(decodedJWT, TokenDTO.class);
        if (tokenDTO == null) {
            throw new BizEx(CodeEnum.AUTH_ERR);
        }
        // 把token信息放到请求上下文中
        UserContext.set(tokenDTO);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.remove();
    }
}
