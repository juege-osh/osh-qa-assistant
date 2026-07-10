package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlertSelfCheckVO {
    private String scene;
    private String title;
    private String status;
    private String operator;
    private Boolean alertEnabled;
    private Boolean mailSenderReady;
    private String subjectPrefix;
    private String from;
    private List<String> recipients;
    private String message;
}
