package com.osh.ai.assistant.common.util;


import com.osh.ai.assistant.common.bean.req.BasePageReq;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页工具类
 */
public class PageUtil {
    /**
     * 构建IPage类型的对象
     */
    public static <T> IPage<T> buildPage(BasePageReq pageReq) {
        return new Page<>(pageReq.getPageNow(),pageReq.getPageSize());
    }
}
