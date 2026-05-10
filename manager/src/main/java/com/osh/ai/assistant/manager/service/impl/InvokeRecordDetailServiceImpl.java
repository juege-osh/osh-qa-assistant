package com.osh.ai.assistant.manager.service.impl;

import com.osh.ai.assistant.common.bean.entity.InvokeRecordDetailDO;
import com.osh.ai.assistant.manager.mapper.InvokeRecordDetailMapper;
import com.osh.ai.assistant.manager.service.InvokeRecordDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
/**
 * 调用记录明细业务实现类

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvokeRecordDetailServiceImpl extends ServiceImpl<InvokeRecordDetailMapper,InvokeRecordDetailDO> implements InvokeRecordDetailService {

}
