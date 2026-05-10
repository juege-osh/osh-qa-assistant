package com.osh.ai.assistant.common.config;

import com.osh.ai.assistant.common.handler.MpMetaObjectHandler;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * @author zhaodaowen
 * @see <a href="https://example.invalid">项目维护者</a>
 */
public class MybatisPlusConfig {
    /**
     * 插件注册的一种方式,注解返回bean到ioc容器即可.
     * 配置mybatis的分页拦截器,不然selectPage不会加limit
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MpMetaObjectHandler mpMetaObjectHandler () {
        return new MpMetaObjectHandler();
    }
}
