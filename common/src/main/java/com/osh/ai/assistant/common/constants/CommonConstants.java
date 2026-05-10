package com.osh.ai.assistant.common.constants;
import java.nio.charset.StandardCharsets;

public class CommonConstants {
    public static final Long ZERO_LONG = 0L;
    public static final Long DEFAULT_PAGE_NOW = 1L;
    public static final Long DEFAULT_PAGE_SIZE = 10L;
    public static final String UTF_8_NAME = StandardCharsets.UTF_8.name();
    public static final String DOT = ".";
    public static final String COMMA = ",";
    public static final String SLASH = "/";
    public static final String UNDER_LINE = "_";
    public static final String RESOURCES_PREFIX = "resources/";
    public static final String UPLOAD_DATE_FORMAT = "yyyyMMdd/";
    public static final String LIB_ID_STR_KEY = "libId";
    /**
     * 验证码过期时间
     */
    public static final Integer CAPTCHA_EXPIRE_IN_MINUTES = 5;
    public static final Integer TOP_K = 200;
}
