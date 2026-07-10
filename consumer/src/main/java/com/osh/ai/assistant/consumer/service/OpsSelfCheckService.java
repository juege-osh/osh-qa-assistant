package com.osh.ai.assistant.consumer.service;

import com.osh.ai.assistant.consumer.bean.req.ops.AlertSelfCheckReq;
import com.osh.ai.assistant.consumer.bean.vo.AlertReadinessVO;
import com.osh.ai.assistant.consumer.bean.vo.AlertSelfCheckVO;

public interface OpsSelfCheckService {
    AlertReadinessVO queryAlertReadiness();

    AlertSelfCheckVO triggerAlertSelfCheck(AlertSelfCheckReq req);
}
