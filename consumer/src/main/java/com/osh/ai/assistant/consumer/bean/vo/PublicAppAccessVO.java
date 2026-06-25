package com.osh.ai.assistant.consumer.bean.vo;

import lombok.Data;

/**
 * 公开应用访问授权结果
 */
@Data
public class PublicAppAccessVO {
    /**
     * 临时访问凭证
     */
    private String accessToken;
    /**
     * 过期秒数
     */
    private Long expireSeconds;
}
