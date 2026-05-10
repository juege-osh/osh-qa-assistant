package com.osh.ai.assistant.common.bean.vo;

import lombok.Data;

/**
 * 验证码
 */
@Data
public class CodeVO {
    /**
     * 标识
     */
    private String captchaId;
    /**
     * base64编码,带"data:image/png;base64,"前缀
     */
    private String text;
}
