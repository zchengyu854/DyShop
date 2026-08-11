package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dyshop.api.mapper.OrderMapper;
import com.dyshop.api.mapper.ProductMapper;
import com.dyshop.api.mapper.UserMapper;
import com.dyshop.api.vo.OverviewVO;
import com.dyshop.api.vo.TrendVO;
import com.dyshop.common.entity.Order;
import com.dyshop.common.entity.Product;
import com.dyshop.common.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台仪表盘统计实现。
 * <p>
 * 统计口径（与 docs/ch08/spec.md D6 一致）：
 * - 今日订单数：orders.create_time 为今日（含待支付/已取消）
 * - 今日交易额：pay_time 为今日 且 status∈1/2/3（不含已取消）Σ pay_amount
 * - 趋势：订单数按 create_time 聚合；交易额按 pay_time 聚合，空天补 0
 */
@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public OverviewVO overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        // 今日订单数
        Long todayOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart)
                .lt(Order::getCreateTime, tomorrowStart));

        // 今日交易额：pay_time 今日 + 已支付成功（status 1/2/3）
        List<Order> todayPaid = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, 1, 2, 3)
                .ge(Order::getPayTime, todayStart)
                .lt(Order::getPayTime, tomorrowStart));
        BigDecimal todayPaidAmount = todayPaid.stream()
                .map(Order::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long waitPayCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 0));
        Long waitShipCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 1));
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>());
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>());

        OverviewVO vo = new OverviewVO();
        vo.setTodayOrderCount(todayOrderCount == null ? 0 : todayOrderCount);
        vo.setTodayPaidAmount(todayPaidAmount == null ? BigDecimal.ZERO : todayPaidAmount);
        vo.setWaitPayCount(waitPayCount == null ? 0 : waitPayCount);
        vo.setWaitShipCount(waitShipCount == null ? 0 : waitShipCount);
        vo.setProductCount(productCount == null ? 0 : productCount);
        vo.setUserCount(userCount == null ? 0 : userCount);
        return vo;
    }

    public TrendVO trend(String days) {
        // 起止日期（含当天）；days=all 时取最早订单创建日
        LocalDate end = LocalDate.now();
        LocalDate start;
        if ("all".equalsIgnoreCase(days)) {
            Order earliest = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .orderByAsc(Order::getCreateTime)
                    .last("LIMIT 1"));
            start = earliest == null || earliest.getCreateTime() == null
                    ? end.minusDays(6)
                    : earliest.getCreateTime().toLocalDate();
        } else {
            int n = "30".equals(days) ? 30 : 7;
            start = end.minusDays(n - 1L);
        }

        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dates.add(d);
        }

        // 订单数：create_time 按日聚合
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, start.atStartOfDay())
                .lt(Order::getCreateTime, end.plusDays(1).atStartOfDay()));
        Map<LocalDate, Long> orderByDay = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCreateTime().toLocalDate(), Collectors.counting()));

        // 交易额：pay_time 按日聚合（status 1/2/3）
        List<Order> paid = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, 1, 2, 3)
                .isNotNull(Order::getPayTime)
                .ge(Order::getPayTime, start.atStartOfDay())
                .lt(Order::getPayTime, end.plusDays(1).atStartOfDay()));
        Map<LocalDate, BigDecimal> amountByDay = paid.stream()
                .collect(Collectors.groupingBy(o -> o.getPayTime().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getPayAmount, BigDecimal::add)));

        TrendVO vo = new TrendVO();
        vo.setDates(dates.stream().map(DAY::format).toList());
        vo.setOrderCounts(dates.stream()
                .map(d -> orderByDay.getOrDefault(d, 0L)).toList());
        vo.setPaidAmounts(dates.stream()
                .map(d -> amountByDay.getOrDefault(d, BigDecimal.ZERO)).toList());
        return vo;
    }
}