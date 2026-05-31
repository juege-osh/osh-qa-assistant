package com.osh.ai.assistant.consumer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.osh.ai.assistant.common.bean.entity.InvokeRecordDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.consumer.bean.vo.InvokeRecordOverviewVO;
import com.osh.ai.assistant.consumer.mapper.InvokeRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final InvokeRecordMapper invokeRecordMapper;

    /**
     * 获取当前用户的调用统计
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Long userId = UserContext.getUserId();
        Map<String, Object> stats = new HashMap<>();
        InvokeRecordOverviewVO overview = invokeRecordMapper.queryOverview(userId);

        long totalCalls = overview != null && overview.getTotalCount() != null ? overview.getTotalCount() : 0L;
        long successCalls = overview != null && overview.getSuccessCount() != null ? overview.getSuccessCount() : 0L;
        long failCalls = overview != null && overview.getFailCount() != null ? overview.getFailCount() : 0L;
        long totalCostToken = overview != null && overview.getTotalCostToken() != null ? overview.getTotalCostToken() : 0L;
        long avgCostTime = overview != null && overview.getAvgCostTime() != null ? overview.getAvgCostTime() : 0L;

        stats.put("totalCalls", totalCalls);
        stats.put("successCalls", successCalls);
        stats.put("failCalls", failCalls);
        stats.put("totalCostToken", totalCostToken);
        stats.put("avgCostTime", avgCostTime);

        double successRate = totalCalls > 0 ? (double) successCalls / totalCalls * 100 : 0;
        stats.put("successRate", Math.round(successRate * 10) / 10.0);

        Long todayCalls = invokeRecordMapper.selectCount(
            new LambdaQueryWrapper<InvokeRecordDO>()
                .eq(InvokeRecordDO::getUserId, userId)
                .ge(InvokeRecordDO::getStartTime, getToday())
        );
        stats.put("todayCalls", todayCalls);

        return Result.buildSuccess(stats);
    }

    private Date getToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
