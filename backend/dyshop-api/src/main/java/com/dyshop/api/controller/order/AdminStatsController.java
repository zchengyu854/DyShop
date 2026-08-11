package com.dyshop.api.controller.order;

import com.dyshop.api.service.impl.AdminStatsServiceImpl;
import com.dyshop.api.vo.OverviewVO;
import com.dyshop.api.vo.TrendVO;
import com.dyshop.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台仪表盘统计接口（需 ROLE_ADMIN）。
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsServiceImpl adminStatsService;

    @GetMapping("/overview")
    public Result<OverviewVO> overview() {
        return Result.success(adminStatsService.overview());
    }

    @GetMapping("/trend")
    public Result<TrendVO> trend(@RequestParam(defaultValue = "7") String days) {
        return Result.success(adminStatsService.trend(days));
    }
}