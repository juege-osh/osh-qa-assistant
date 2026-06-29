package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlertReadinessVO {
    private Boolean alertEnabled;
    private Boolean selfCheckEnabled;
    private Boolean mailSenderReady;
    private Boolean smtpHostConfigured;
    private Boolean smtpUsernameConfigured;
    private Boolean recipientsConfigured;
    private String subjectPrefix;
    private String from;
    private List<String> recipients;
    private List<String> allowedUsers;
    private String status;
    private String message;
}
