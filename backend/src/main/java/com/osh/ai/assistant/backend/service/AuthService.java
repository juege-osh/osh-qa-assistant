package com.osh.ai.assistant.backend.service;

import com.osh.ai.assistant.backend.bean.req.AuthLoginReq;
import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;

public interface AuthService {
    CodeVO getCode();

    LoginResultVO login(AuthLoginReq loginReq);
}
