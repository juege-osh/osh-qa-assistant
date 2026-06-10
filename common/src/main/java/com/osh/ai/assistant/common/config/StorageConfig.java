package com.osh.ai.assistant.common.config;

import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.controller.StorageController;
import com.osh.ai.assistant.common.service.OssService;
import com.osh.ai.assistant.common.service.StorageService;
import com.osh.ai.assistant.common.service.impl.OssServiceImpl;
import com.osh.ai.assistant.common.service.impl.StorageServiceImpl;
import com.osh.ai.assistant.common.util.OssUtil;
import jakarta.annotation.Resource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author zhaodaowen
 * @see <a href="https://example.invalid">项目维护者</a>
 */
@EnableConfigurationProperties(UploadProperties.class)
public class StorageConfig implements WebMvcConfigurer {

    @Resource
    private UploadProperties uploadProperties;
    /**
     * 静态资源的位置增加指定的file目录
     * ,使得可以通过 http://localhost:9000/resources/images/20240601/123.jpg
     * 进行访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String[] locations = { "classpath:/META-INF/resources/",
                "classpath:/resources/", "classpath:/static/", "classpath:/public/",
                // 自定义的本地路径
                "file:"+ uploadProperties.getStaticDir()};
        registry.addResourceHandler("/**")
                .addResourceLocations(locations);
    }

    @Bean
    public StorageService storageService() {
        return new StorageServiceImpl();
    }

    @Bean
    public OssService ossService() {
        return new OssServiceImpl();
    }

    @Bean
    public OssUtil ossUtil() {
        return new OssUtil();
    }

    @Bean
    public StorageController storageController() {
        return new StorageController();
    }
}
