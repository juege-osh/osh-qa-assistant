package com.osh.ai.assistant.consumer.mapper;

import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.consumer.bean.req.app.AppPageReq;
import com.osh.ai.assistant.consumer.bean.vo.AppVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 应用信息mapper类

 */
public interface AppMapper extends BaseMapper<AppDO> {

    Long queryCount(AppPageReq pageReq);

    List<AppVO> queryList(AppPageReq pageReq);
}
