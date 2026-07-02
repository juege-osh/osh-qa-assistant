package com.osh.ai.assistant.consumer.bean.dto;

import lombok.Data;

/**
 * 公开应用密码访问令牌
 */
@Data
public class PublicAppAccessTokenDTO {
    /**
     * 公开访问标识
     */
    private String slug;
    /**
     * 签发时对应的密码哈希
     */
    private String passwordHash;
}
