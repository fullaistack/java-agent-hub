package com.agenthub.tracing.controller;

import com.agenthub.common.api.ApiResponse;
import com.agenthub.tracing.entity.CallStat;
import com.agenthub.tracing.mapper.CallStatMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 链路统计接口：调用统计明细与聚合指标查询。
 */
@RestController
@RequestMapping("/api/v1/tracing")
@RequiredArgsConstructor
public class TraceController {

    private final CallStatMapper callStatMapper;

    /** 按类型聚合统计（最近 N 小时，默认 24） */
    @GetMapping("/stats")
    public ApiResponse<List<Map<String, Object>>> stats(@RequestParam(defaultValue = "24") int hours) {
        return ApiResponse.ok(callStatMapper.aggregateByType(hours));
    }

    /** 最近调用明细（最多 200 条） */
    @GetMapping("/calls")
    public ApiResponse<List<CallStat>> calls(@RequestParam(required = false) String bizType) {
        return ApiResponse.ok(callStatMapper.selectList(new LambdaQueryWrapper<CallStat>()
                .eq(bizType != null && !bizType.isBlank(), CallStat::getBizType, bizType)
                .orderByDesc(CallStat::getId)
                .last("LIMIT 200")));
    }
}
