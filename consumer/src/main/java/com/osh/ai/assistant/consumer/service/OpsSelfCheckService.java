package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.consumer.bean.req.ops.AlertSelfCheckReq;
import com.osh.ai.assistant.consumer.bean.vo.AlertSelfCheckVO;

public interface OpsSelfCheckService {
    AlertSelfCheckVO triggerAlertSelfCheck(AlertSelfCheckReq req);
}
