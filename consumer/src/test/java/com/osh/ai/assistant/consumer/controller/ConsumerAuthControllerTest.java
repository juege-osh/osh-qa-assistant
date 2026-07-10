package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.vo.CodeVO;
import com.osh.ai.assistant.consumer.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
