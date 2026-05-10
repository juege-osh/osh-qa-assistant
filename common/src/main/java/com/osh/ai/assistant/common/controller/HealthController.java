package com.osh.ai.assistant.common.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 返回应用的健康状态
 */
@RestController
public class HealthController {

    @GetMapping("/")
    public Result<String> health(){
        return Result.buildSuccess("UP");
    }
}
