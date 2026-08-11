package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.dto.AfterSaleApplyDTO;
import com.dyshop.api.mapper.AfterSaleMapper;
import com.dyshop.api.mapper.OrderItemMapper;
import com.dyshop.api.mapper.OrderMapper;
import com.dyshop.api.mapper.UserMapper;
import com.dyshop.api.vo.AdminAfterSaleVO;
import com.dyshop.api.vo.AfterSaleVO;
import com.dyshop.common.entity.AfterSale;
import com.dyshop.common.entity.Order;
import com.dyshop.common.entity.OrderItem;
import com.dyshop.common.entity.User;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.PageResult;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 售后/退款实现（ch12）。
 */
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String[] STATUS_TEXT = {"待处理", "退款中", "已退款", "已拒绝", "已取消"};

    private final AfterSaleMapper afterSaleMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public AfterSaleVO apply(Long userId, AfterSaleApplyDTO dto) {
        OrderItem item = orderItemMapper.selectById(dto.getOrderItemId());
        if (item == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        // R1：仅已完成订单可申请售后
        if (!Objects.equals(order.getStatus(), 3)) {
            throw new BizException(ResultCode.PARAM_ERROR, "仅已完成订单可申请售后");
        }
        // R3：防重复（预检 + 唯一索引兜底）
        Long exists = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderItemId, item.getId()));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.AFTER_SALE_DUPLICATED);
        }

        AfterSale as = new AfterSale();
        as.setAfterSaleNo(generateNo());
        as.setOrderId(order.getId());
        as.setOrderItemId(item.getId());
        as.setUserId(userId);
        as.setProductId(item.getProductId());
        as.setProductName(item.getProductName());
        as.setProductImage(item.getProductImage());
        as.setSpecText(item.getSpecText());
        as.setQuantity(item.getQuantity());
        // R2：退款金额 = 成交单价 × 数量（后端自动计算，前端只读）
        as.setRefundAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        as.setReason(StringUtils.hasText(dto.getReason()) ? dto.getReason().trim() : "其他");
        as.setType("ONLY_REFUND");
        as.setStatus(0);
        as.setCreateTime(LocalDateTime.now());
        try {
            afterSaleMapper.insert(as);
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCode.AFTER_SALE_DUPLICATED);
        }
        return toVO(as);
    }

    public PageResult<AfterSaleVO> mine(Long userId, Integer status, long page, long size) {
        LambdaQueryWrapper<AfterSale> qw = new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getUserId, userId)
                .eq(status != null, AfterSale::getStatus, status)
                .orderByDesc(AfterSale::getCreateTime);
        IPage<AfterSale> p = afterSaleMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getRecords().stream().map(this::toVO).toList(),
                p.getTotal(), p.getCurrent(), p.getSize());
    }

    public AfterSaleVO detail(Long userId, Long id) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null || !Objects.equals(as.getUserId(), userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "售后单不存在");
        }
        return toVO(as);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null || !Objects.equals(as.getUserId(), userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "售后单不存在");
        }
        // R4：仅待处理可取消；已处理幂等返回（视为已取消无需再操作）
        if (!Objects.equals(as.getStatus(), 0)) {
            throw new BizException(ResultCode.PARAM_ERROR, "当前状态不可取消");
        }
        as.setStatus(4);
        as.setCancelTime(LocalDateTime.now());
        afterSaleMapper.updateById(as);
    }

    public IPage<AdminAfterSaleVO> adminList(Integer status, String keyword, long page, long size) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        final List<Long> kwOrderIds;
        final List<Long> kwUserIds;
        if (hasKeyword) {
            kwOrderIds = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                            .like(Order::getOrderNo, keyword.trim()))
                    .stream().map(Order::getId).toList();
            kwUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getUsername, keyword.trim()))
                    .stream().map(User::getId).toList();
        } else {
            kwOrderIds = List.of();
            kwUserIds = List.of();
        }

        LambdaQueryWrapper<AfterSale> qw = new LambdaQueryWrapper<AfterSale>()
                .eq(status != null, AfterSale::getStatus, status)
                .and(hasKeyword, w -> w
                        .in(!kwOrderIds.isEmpty(), AfterSale::getOrderId, kwOrderIds)
                        .or().in(!kwUserIds.isEmpty(), AfterSale::getUserId, kwUserIds)
                        .or().like(StringUtils.hasText(keyword), AfterSale::getProductName, keyword == null ? null : keyword.trim()))
                .orderByDesc(AfterSale::getCreateTime);
        IPage<AfterSale> p = afterSaleMapper.selectPage(new Page<>(page, size), qw);
        if (p.getRecords().isEmpty()) {
            return new Page<>(page, size);
        }
        Map<Long, Order> orders = orderMapper.selectBatchIds(
                        p.getRecords().stream().map(AfterSale::getOrderId).distinct().toList())
                .stream().collect(Collectors.toMap(Order::getId, Function.identity()));
        Map<Long, User> users = userMapper.selectBatchIds(
                        p.getRecords().stream().map(AfterSale::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        Page<AdminAfterSaleVO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(p.getRecords().stream().map(as -> {
            AdminAfterSaleVO vo = new AdminAfterSaleVO();
            copyTo(as, vo);
            Order o = orders.get(as.getOrderId());
            vo.setOrderNo(o == null ? null : o.getOrderNo());
            User u = users.get(as.getUserId());
            vo.setUsername(u == null ? null : u.getUsername());
            return vo;
        }).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        // R5：仅待处理可同意；模拟退款直接置已退款完成
        int rows = afterSaleMapper.update(null, new LambdaUpdateWrapper<AfterSale>()
                .eq(AfterSale::getId, id)
                .eq(AfterSale::getStatus, 0)
                .set(AfterSale::getStatus, 2)
                .set(AfterSale::getHandleTime, LocalDateTime.now()));
        if (rows == 0 && afterSaleMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "售后单不存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BizException(ResultCode.PARAM_ERROR, "请填写拒绝理由");
        }
        int rows = afterSaleMapper.update(null, new LambdaUpdateWrapper<AfterSale>()
                .eq(AfterSale::getId, id)
                .eq(AfterSale::getStatus, 0)
                .set(AfterSale::getStatus, 3)
                .set(AfterSale::getRejectReason, reason.trim())
                .set(AfterSale::getHandleTime, LocalDateTime.now()));
        if (rows == 0 && afterSaleMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "售后单不存在");
        }
    }

    // ---------- 私有 ----------

    private String generateNo() {
        for (int i = 0; i < 3; i++) {
            String no = "AS" + LocalDateTime.now().format(NO_FORMAT)
                    + String.format("%08d", ThreadLocalRandom.current().nextInt(100000000));
            Long count = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                    .eq(AfterSale::getAfterSaleNo, no));
            if (count == null || count == 0) {
                return no;
            }
        }
        throw new BizException(ResultCode.ERROR, "售后单号生成失败，请重试");
    }

    private AfterSaleVO toVO(AfterSale as) {
        AfterSaleVO vo = new AfterSaleVO();
        copyTo(as, vo);
        return vo;
    }

    private void copyTo(AfterSale as, AfterSaleVO vo) {
        vo.setId(as.getId());
        vo.setAfterSaleNo(as.getAfterSaleNo());
        vo.setOrderId(as.getOrderId());
        vo.setOrderItemId(as.getOrderItemId());
        vo.setProductId(as.getProductId());
        vo.setProductName(as.getProductName());
        vo.setProductImage(as.getProductImage());
        vo.setSpecText(as.getSpecText());
        vo.setQuantity(as.getQuantity());
        vo.setRefundAmount(as.getRefundAmount());
        vo.setReason(as.getReason());
        vo.setType(as.getType());
        vo.setStatus(as.getStatus());
        vo.setStatusText(statusText(as.getStatus()));
        vo.setRejectReason(as.getRejectReason());
        vo.setHandleTime(as.getHandleTime());
        vo.setCancelTime(as.getCancelTime());
        vo.setCreateTime(as.getCreateTime());
    }

    private String statusText(Integer status) {
        if (status == null || status < 0 || status >= STATUS_TEXT.length) {
            return "未知";
        }
        return STATUS_TEXT[status];
    }
}
