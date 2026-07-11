package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.common.bean.vo.LoginResultVO;
import com.osh.ai.assistant.consumer.bean.req.user.LoginReq;
import com.osh.ai.assistant.consumer.bean.req.user.UserRegisterReq;
import com.osh.ai.assistant.consumer.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsumerAuthControllerTest {

    @Test
    void getCodeShouldBeMappedToConsumerAuthEndpoint() throws Exception {
        UserService userService = org.mockito.Mockito.mock(UserService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ConsumerAuthController(userService)).build();
        CodeVO code = new CodeVO();
        code.setCaptchaId("captcha-id");
        code.setText("data:image/png;base64,test");
        when(userService.getCode()).thenReturn(code);

        mockMvc.perform(get("/auth/getCode"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.captchaId").value("captcha-id"));
    }

    @Test
    void loginShouldDelegateToUserService() throws Exception {
        UserService userService = org.mockito.Mockito.mock(UserService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ConsumerAuthController(userService)).build();
        LoginResultVO loginResult = new LoginResultVO();
        loginResult.setToken("token");
        when(userService.login(any(LoginReq.class))).thenReturn(loginResult);

        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("""
                    {"username":"tester","pwd":"password","captchaId":"captcha-id","code":"1234"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").value("token"));

        verify(userService).login(any(LoginReq.class));
    }

    @Test
    void registerShouldDelegateToUserService() throws Exception {
        UserService userService = org.mockito.Mockito.mock(UserService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ConsumerAuthController(userService)).build();
        doNothing().when(userService).register(any(UserRegisterReq.class));

        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content("""
                    {"username":"tester","pwd":"password","captchaId":"captcha-id","code":"1234"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.msg").value("注册成功"));

        verify(userService).register(any(UserRegisterReq.class));
    }
}
