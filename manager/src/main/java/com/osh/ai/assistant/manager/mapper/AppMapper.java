package com.osh.ai.assistant.manager.mapper;
import com.osh.ai.assistant.manager.bean.req.app.AppPageReq;
import com.osh.ai.assistant.manager.bean.vo.AppVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.osh.ai.assistant.common.bean.entity.AppDO;

import java.util.List;

/**
 * 应用信息mapper类

 */
public interface AppMapper extends BaseMapper<AppDO> {

    Long queryCount(AppPageReq pageReq);

    List<AppVO> queryList(AppPageReq pageReq);
}
