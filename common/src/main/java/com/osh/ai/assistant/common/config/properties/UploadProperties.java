package com.osh.ai.assistant.common.config.properties;

import cn.hutool.core.util.StrUtil;
import com.osh.ai.assistant.common.util.PathUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;



@ConfigurationProperties(prefix = "upload")
@Data
public class UploadProperties {
    /**
     * 存储文件的绝对路径,格式如: e:/a/
     */
    private String staticDir;

    @PostConstruct
    public void init() {
        if (StrUtil.isBlank(staticDir)) {
            throw new RuntimeException("UploadProperties.staticDir未配置");
        }
        staticDir = PathUtil.normalize(staticDir);
        PathUtil.checkAndCreateIfNotExist(staticDir);
    }
}
