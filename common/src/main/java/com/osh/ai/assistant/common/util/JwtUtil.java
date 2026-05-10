package com.osh.ai.assistant.common.util;

import cn.hutool.json.JSONUtil;
import com.osh.ai.assistant.common.enums.CodeEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Calendar;

/**
 * jwt工具类
 *
 */
public class JwtUtil {
    private static final String SECRET = "osh-ai-assistant-jwt-secret";

    /**
     * 生成token
     */
    public static <T> String getToken(T t) {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MONTH, 1);

        JWTCreator.Builder builder = JWT.create()
                // 添加声明 id:123,name:abc,userInfo:xxx,TokenDTO:xxx
                .withClaim(t.getClass().getSimpleName(), JSONUtil.toJsonStr(t));
        // 指定什么时候过期
        return builder.withExpiresAt(instance.getTime())
                // 签发token
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 用来校验token是否合法
     */
    public static DecodedJWT verify(String tokenToVerify) {
        String errMsg;
        try {
            return JWT.require(Algorithm.HMAC256(SECRET))
                    .build().verify(tokenToVerify);
        } catch (AlgorithmMismatchException e) {
            errMsg = "算法不匹配";
        } catch (InvalidClaimException e) {
            errMsg = "声明无效";
        } catch (SignatureVerificationException e) {
            errMsg = "签名不匹配";
        } catch (TokenExpiredException e) {
            errMsg = "令牌失效";
        } catch (Exception e) {
            errMsg = "校验令牌失败";
        }
        throw new BizEx(CodeEnum.AUTH_ERR);
    }

    /**
     * 解析decodedJWT为指定的clazz对象(从token中获取信息)
     */
    public static <T> T parse(DecodedJWT decodedJWT,Class<T> clazz) {
        Claim claim = decodedJWT.getClaim(clazz.getSimpleName());
        if (claim == null) {
            return null;
        }
        return JSONUtil.toBean(claim.asString(),clazz);
    }
}
