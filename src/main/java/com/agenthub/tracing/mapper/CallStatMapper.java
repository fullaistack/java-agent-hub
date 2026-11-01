package com.agenthub.tracing.mapper;

import com.agenthub.tracing.entity.CallStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 调用统计 Mapper。
 */
public interface CallStatMapper extends BaseMapper<CallStat> {

    /** 按类型聚合：调用次数、平均耗时、token 总量（最近 N 小时） */
    @Select("""
            SELECT biz_type AS bizType,
                   COUNT(*)   AS total,
                   AVG(cost_ms) AS avgCostMs,
                   COALESCE(SUM(prompt_tokens), 0) AS promptTokens,
                   COALESCE(SUM(completion_tokens), 0) AS completionTokens,
                   SUM(success = 1) AS successCount
            FROM call_stat
            WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)
            GROUP BY biz_type
            """)
    List<Map<String, Object>> aggregateByType(@Param("hours") int hours);
}
