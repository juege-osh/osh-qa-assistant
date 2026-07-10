package com.osh.ai.assistant.consumer.service;

public interface AlertService {
    void notifyOnce(String key, String title, String body);

    void notifyResolved(String key, String title, String body);

    void sendSelfCheck(String title, String body);

    boolean canSendAlertNow();
}
