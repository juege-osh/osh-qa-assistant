package com.osh.ai.assistant.common.config;

import com.osh.ai.assistant.common.config.properties.UploadProperties;
import com.osh.ai.assistant.common.controller.StorageController;
import jakarta.annotation.Resource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 存储模块配置类。
 * <p>
 * StorageService / OssService / OssUtil 已在各自类上标注了 @Service / @Component，
 * 会被 Spring 组件扫描自动注册，无需在此重复声明 @Bean。
 * <p>
 * 注意：此类不加 @Configuration，因为 backend 统一模式下通过组件扫描自动加载会导致 Bean 冲突。
 * 在 consumer/manager 独立模式下，通过各自 CommonConfig 的 @Import 显式加载。
 *
 * @author zhaodaowen
 */
@EnableConfigurationProperties(UploadProperties.class)
public class StorageConfig implements WebMvcConfigurer {

    @Resource
    private UploadProperties uploadProperties;

    /**
     * 静态资源的位置增加指定的file目录,
     * 使得可以通过 http://localhost:9000/resources/images/20240601/123.jpg 进行访问
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
    public StorageController storageController() {
        return new StorageController();
    }
}
