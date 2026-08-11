package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.dto.CreateOrderDTO;
import com.dyshop.api.mapper.AddressMapper;
import com.dyshop.api.mapper.CartItemMapper;
import com.dyshop.api.mapper.OrderItemMapper;
import com.dyshop.api.mapper.OrderMapper;
import com.dyshop.api.mapper.PaymentMapper;
import com.dyshop.api.mapper.ProductMapper;
import com.dyshop.api.mapper.CouponTemplateMapper;
import com.dyshop.api.mapper.OrderCouponMapper;
import com.dyshop.api.mapper.AfterSaleMapper;
import com.dyshop.api.mapper.UserCouponMapper;
import com.dyshop.api.mapper.UserMapper;
import com.dyshop.api.service.impl.MemberLevelServiceImpl;
import com.dyshop.api.util.CouponUtils;
import com.dyshop.api.util.SkuJsonUtils;
import com.dyshop.api.vo.AdminOrderVO;
import com.dyshop.api.vo.CouponOptionVO;
import com.dyshop.api.vo.OrderCouponVO;
import com.dyshop.api.vo.OrderItemVO;
import com.dyshop.api.vo.OrderPreviewVO;
import com.dyshop.api.vo.OrderVO;
import com.dyshop.api.vo.UserOrderOverviewVO;
import com.dyshop.api.vo.SkuVO;
import com.dyshop.common.entity.Address;
import com.dyshop.common.entity.CouponTemplate;
import com.dyshop.common.entity.OrderCoupon;
import com.dyshop.common.entity.UserCoupon;
import com.dyshop.common.entity.CartItem;
import com.dyshop.common.entity.MemberLevel;
import com.dyshop.common.entity.Order;
import com.dyshop.common.entity.OrderItem;
import com.dyshop.common.entity.Payment;
import com.dyshop.common.entity.Product;
import com.dyshop.common.entity.AfterSale;
import com.dyshop.common.entity.User;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.ResultCode;
import org.springframework.dao.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String[] STATUS_TEXT = {"待支付", "待发货", "待收货", "已完成", "已取消"};

    /**
     * 待支付订单超时时间（分钟）。
     * 前端 15 分钟倒计时仅作为用户体验层；库存释放由后端兜底：
     * {@code OrderTimeoutScheduler} 定时扫描（60s 间隔）调用
     * {@link #expireTimeoutOrders()}，将超时订单置为已取消并回补库存。
     */
    public static final int PAY_TIMEOUT_MINUTES = 15;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final AddressMapper addressMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final MemberLevelServiceImpl memberLevelService;
    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final OrderCouponMapper orderCouponMapper;
    private final AfterSaleMapper afterSaleMapper;

    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, CreateOrderDTO dto) {
        Address address = requireOwnedAddress(userId, dto.getAddressId());

        // 结算条目（商品 + 数量），cart 模式取勾选项，buyNow 取单件
        List<Line> lines = buildLines(userId, dto);

        // 会员价：下单时刻等级快照，整单统一（ch09）
        MemberLevel level = memberLevelService.getCurrentLevel(userId);

        // 金额 = Σ(SKU 价或商品价 × 数量)，无运费；成交价按会员权益计算
        // 原价合计（商品总额基准）与会员价合计
        BigDecimal baseTotal = BigDecimal.ZERO;
        BigDecimal memberTotal = BigDecimal.ZERO;
        for (Line line : lines) {
            baseTotal = baseTotal.add(basePrice(line).multiply(BigDecimal.valueOf(line.quantity)));
            memberTotal = memberTotal.add(unitPrice(level, line).multiply(BigDecimal.valueOf(line.quantity)));
        }

        // 优惠券（ch11）：可选，一单一券；二选一自动取优（会员价 vs 原价−券额，取更省）
        CouponApply apply = resolveCoupon(userId, dto.getCouponId(), lines, level, baseTotal, memberTotal);
        BigDecimal discount = apply.discount;
        boolean couponApplied = apply.applied;
        BigDecimal payAmount = couponApplied ? baseTotal.subtract(discount) : memberTotal;
        // 总优惠口径：pay_amount = total_amount − discount_amount（会员优惠或券优惠取优后）
        BigDecimal discountAmount = baseTotal.subtract(payAmount);

        Order order = new Order();
        order.setOrderNo(generateNo(""));
        order.setUserId(userId);
        order.setTotalAmount(baseTotal);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(payAmount);
        order.setStatus(0);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddr(fullAddress(address));
        order.setRemark(trimToNull(dto.getRemark()));
        // 显式写入创建时间：insert 不自动回填，返回 VO 的 payDeadline 依赖它
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 乐观扣券：status=0 且未过期才允许扣（影响行数≠1 = 并发已扣/已过期 → 整体回滚）
        if (apply.userCoupon != null && apply.applied) {
            int rows = userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, apply.userCoupon.getId())
                    .eq(UserCoupon::getStatus, 0)
                    .and(w -> w.isNull(UserCoupon::getExpireAt)
                            .or().ge(UserCoupon::getExpireAt, LocalDateTime.now()))
                    .set(UserCoupon::getStatus, 1)
                    .set(UserCoupon::getUsedOrderId, order.getId())
                    .set(UserCoupon::getUsedAt, LocalDateTime.now()));
            if (rows != 1) {
                throw new BizException(ResultCode.COUPON_USED);
            }
            // 订单券快照（order_id 唯一约束二次防重）
            OrderCoupon oc = new OrderCoupon();
            oc.setOrderId(order.getId());
            oc.setUserCouponId(apply.userCoupon.getId());
            oc.setTemplateId(apply.template.getId());
            oc.setTemplateName(apply.template.getName());
            oc.setScope(apply.template.getScope());
            oc.setCategoryIds(apply.template.getCategoryIds());
            oc.setProductIds(apply.template.getProductIds());
            oc.setDiscountAmount(discount);
            oc.setUsedAt(LocalDateTime.now());
            try {
                orderCouponMapper.insert(oc);
            } catch (DuplicateKeyException e) {
                throw new BizException(ResultCode.COUPON_USED, "该优惠券已用于其他订单");
            }
        }

        for (Line line : lines) {
            BigDecimal unitPrice = unitPrice(level, line);
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(line.product.getId());
            item.setSkuId(line.sku != null ? line.sku.getId() : 0L);
            item.setProductName(line.product.getName());
            item.setProductImage(line.product.getMainImage());
            // 规格快照按 SKU 生成（不信任前端直传）；无规格商品为 NULL
            if (line.sku != null) {
                item.setSpecText(SkuJsonUtils.buildSpecText(line.sku,
                        SkuJsonUtils.parseSpecs(line.product.getSpecs())));
            }
            item.setPrice(unitPrice);
            item.setQuantity(line.quantity);
            item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(line.quantity)));
            orderItemMapper.insert(item);

            // 条件扣减库存，防止并发超卖；影响行数 0 说明库存已不足 -> 回滚
            int updated = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, line.product.getId())
                    .ge(Product::getStock, line.quantity)
                    .setSql("stock = stock - " + line.quantity));
            if (updated == 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "「" + line.product.getName() + "」库存不足，请调整数量");
            }
            // SKU 显示库存尽力同步（并发安全真源为上方 product.stock 条件扣减）
            if (line.sku != null) {
                changeSkuStock(line.product.getId(), line.sku.getId(), -line.quantity);
            }
        }

        // cart 模式：删除已结算的购物车条目
        if ("cart".equals(dto.getSource())) {
            List<Long> productIds = lines.stream().map(l -> l.product.getId()).toList();
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .in(CartItem::getProductId, productIds));
        }

        return toVo(order);
    }

    public List<OrderVO> listOrders(Long userId, Integer status) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime));
        return orders.stream().map(this::toVo).toList();
    }

    public UserOrderOverviewVO overview(Long userId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId));
        // 累计消费口径（与会员等级一致）：已支付订单（pay_time 非空，含待发货/待收货/已完成）计消费
        BigDecimal totalConsumption = BigDecimal.ZERO;
        long waitShip = 0;
        long waitReceive = 0;
        for (Order order : orders) {
            if (order.getPayTime() != null) {
                totalConsumption = totalConsumption.add(order.getPayAmount() == null
                        ? BigDecimal.ZERO : order.getPayAmount());
            }
            // 待发货/待收货按状态独立统计（已支付订单同样计入），
            // 与累计消费口径互不干扰 —— 否则 state=1/2 且 pay_time 非空的订单永远不计入角标
            if (Objects.equals(order.getStatus(), 1)) {
                waitShip++;
            } else if (Objects.equals(order.getStatus(), 2)) {
                waitReceive++;
            }
        }
        // 已退款售后单（status=2）金额从累计消费中扣减
        List<AfterSale> refunds = afterSaleMapper.selectList(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getUserId, userId)
                .eq(AfterSale::getStatus, 2));
        for (AfterSale r : refunds) {
            if (r.getRefundAmount() != null) {
                totalConsumption = totalConsumption.subtract(r.getRefundAmount());
            }
        }
        UserOrderOverviewVO vo = new UserOrderOverviewVO();
        vo.setTotalConsumption(totalConsumption);
        vo.setTotalOrders((long) orders.size());
        vo.setWaitShip(waitShip);
        vo.setWaitReceive(waitReceive);
        return vo;
    }

    public OrderVO getOrder(Long userId, Long id) {
        return toVo(requireOwnedOrder(userId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        Order order = requireOwnedOrder(userId, id);
        // 幂等：已是已取消（含并发/重复提交/超时重试）直接返回成功，不做二次库存回补
        if (Objects.equals(order.getStatus(), 4)) {
            return;
        }
        if (!Objects.equals(order.getStatus(), 0)) {
            throw new BizException(ResultCode.PARAM_ERROR, "订单状态不允许取消");
        }
        restoreStock(order);
        refundCoupon(order);
        order.setStatus(4);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, Long id) {
        Order order = requireOwnedOrder(userId, id);
        // 幂等：已支付（含重复提交/超时重试）直接返回成功，不重复插入支付单、不重复累加销量
        if (Objects.equals(order.getStatus(), 1)) {
            return;
        }
        if (!Objects.equals(order.getStatus(), 0)) {
            throw new BizException(ResultCode.PARAM_ERROR, "订单状态不允许支付");
        }

        Payment payment = new Payment();
        payment.setPaymentNo(generateNo("P"));
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setAmount(order.getPayAmount());
        payment.setChannel("MOCK");
        payment.setStatus(1);
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        // 支付成功：订单转待发货 + 商品销量累加
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        for (OrderItem item : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()))) {
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, item.getProductId())
                    .setSql("sales = sales + " + item.getQuantity()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long userId, Long id) {
        Order order = requireOwnedOrder(userId, id);
        // 幂等：已完成（含重复提交/超时重试）直接返回成功，不重复发放积分（point_log 唯一键兜底）
        if (Objects.equals(order.getStatus(), 3)) {
            return;
        }
        if (!Objects.equals(order.getStatus(), 2)) {
            throw new BizException(ResultCode.PARAM_ERROR, "订单状态不允许确认收货");
        }
        order.setStatus(3);
        order.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(order);
        // 订单完成发放会员积分（ch09；幂等由 point_log.order_id 唯一键兜底）
        memberLevelService.grantPoints(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        // 幂等：订单不存在（含已逻辑删除/重复提交）直接返回成功
        if (order == null) {
            return;
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        // 仅终态（已完成/已取消）允许删除，避免交易中的订单被误清理
        if (!Objects.equals(order.getStatus(), 3) && !Objects.equals(order.getStatus(), 4)) {
            throw new BizException(ResultCode.PARAM_ERROR, "仅已完成或已取消的订单可删除");
        }
        // @TableLogic：deleteById 自动转为逻辑删除（deleted=1），
        // 列表/详情查询由 MyBatis-Plus 自动过滤，无需物理删除明细表。
        orderMapper.deleteById(order.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public int expireTimeoutOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PAY_TIMEOUT_MINUTES);
        List<Order> expired = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 0)
                .le(Order::getCreateTime, cutoff)
                .last("LIMIT 200"));
        int count = 0;
        for (Order order : expired) {
            // 条件更新 status 0→4：并发支付成功/另一实例已处理时影响行数 0，跳过回补
            int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                    .eq(Order::getId, order.getId())
                    .eq(Order::getStatus, 0)
                    .set(Order::getStatus, 4)
                    .set(Order::getCancelTime, LocalDateTime.now()));
            if (updated == 0) {
                continue;
            }
            restoreStock(order);
            refundCoupon(order);
            count++;
        }
        return count;
    }

    // ---------- 后台 ----------

    public IPage<AdminOrderVO> adminList(Integer status, String keyword, long page, long size) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
                .eq(status != null, Order::getStatus, status)
                // 搜索：订单号模糊 OR 收货手机号精确
                .and(hasKeyword, w -> w.like(Order::getOrderNo, keyword.trim())
                        .or().eq(Order::getReceiverPhone, keyword.trim()))
                .orderByDesc(Order::getCreateTime);
        IPage<Order> p = orderMapper.selectPage(new Page<>(page, size), qw);
        if (p.getRecords().isEmpty()) {
            return new Page<>(page, size);
        }
        Map<Long, String> userNames = fetchUserNames(p.getRecords());
        List<AdminOrderVO> vos = p.getRecords().stream()
                .map(o -> toAdminVo(o, userNames.get(o.getUserId())))
                .toList();
        Page<AdminOrderVO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(vos);
        return result;
    }

    public AdminOrderVO adminGet(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        Map<Long, String> userNames = fetchUserNames(List.of(order));
        return toAdminVo(order, userNames.get(order.getUserId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!Objects.equals(order.getStatus(), 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "订单状态不允许发货");
        }
        order.setStatus(2);
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    // ---------- 私有 ----------

    /** 结算行：商品 + SKU（无规格商品为 null）+ 数量 */
    private record Line(Product product, SkuVO sku, int quantity) {
    }

    /** 券应用结果：选中的用户券 + 模板 + 实际抵扣额 + 是否生效（false=自动采用更优的会员价） */
    private record CouponApply(UserCoupon userCoupon, CouponTemplate template,
                               BigDecimal discount, boolean applied) {
    }

    public OrderPreviewVO preview(Long userId, String source, Long couponId,
                                  Long productId, Long skuId, Integer quantity) {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setSource(source == null ? "cart" : source);
        dto.setProductId(productId);
        dto.setSkuId(skuId == null ? 0L : skuId);
        dto.setQuantity(quantity);
        List<Line> lines = buildLines(userId, dto);
        MemberLevel level = memberLevelService.getCurrentLevel(userId);

        // 原价合计（商品总额基准）与会员价合计
        BigDecimal baseTotal = BigDecimal.ZERO;
        BigDecimal memberTotal = BigDecimal.ZERO;
        for (Line line : lines) {
            baseTotal = baseTotal.add(basePrice(line).multiply(BigDecimal.valueOf(line.quantity)));
            memberTotal = memberTotal.add(unitPrice(level, line).multiply(BigDecimal.valueOf(line.quantity)));
        }
        BigDecimal memberBenefit = baseTotal.subtract(memberTotal);

        // 未使用未过期券 → 可用性清单（供结算页渲染选券）
        List<UserCoupon> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0)
                .and(w -> w.isNull(UserCoupon::getExpireAt)
                        .or().gt(UserCoupon::getExpireAt, LocalDateTime.now())));
        Map<Long, CouponTemplate> tplMap = coupons.isEmpty() ? Map.of()
                : couponTemplateMapper.selectBatchIds(
                                coupons.stream().map(UserCoupon::getTemplateId).distinct().toList())
                        .stream().collect(Collectors.toMap(CouponTemplate::getId, Function.identity()));
        List<CouponOptionVO> options = coupons.stream()
                .map(uc -> evaluateCoupon(uc, tplMap.get(uc.getTemplateId()), lines, level))
                .toList();

        BigDecimal discount = BigDecimal.ZERO;
        OrderCouponVO selected = null;
        boolean couponApplied = false;
        if (couponId != null) {
            UserCoupon uc = coupons.stream().filter(c -> c.getId().equals(couponId)).findFirst()
                    .orElseThrow(() -> new BizException(ResultCode.COUPON_USED));
            CouponOptionVO opt = evaluateCoupon(uc, tplMap.get(uc.getTemplateId()), lines, level);
            if (!opt.isApplicable()) {
                throw new BizException(ResultCode.COUPON_INVALID, opt.getReason());
            }
            // 二选一自动取优（ch11 修订）：比较「原价−券额」与「会员价」，用更省的一种
            if (baseTotal.subtract(opt.getDiscount()).compareTo(memberTotal) <= 0) {
                couponApplied = true;
                discount = opt.getDiscount();
                selected = new OrderCouponVO();
                selected.setId(uc.getId());
                selected.setTemplateName(opt.getName());
                selected.setDiscountAmount(discount);
            }
            // 否则会员价更优惠：券不生效，自动采用会员方案
        }

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setLines(lines.stream().map(l -> toLineVO(l, level)).toList());
        vo.setTotalAmount(baseTotal);
        vo.setMemberBenefit(couponApplied ? BigDecimal.ZERO : memberBenefit);
        vo.setCouponDiscount(discount);
        vo.setCouponApplied(couponApplied);
        vo.setPayAmount(couponApplied ? baseTotal.subtract(discount) : memberTotal);
        vo.setCoupon(selected);
        vo.setCouponOptions(options);
        return vo;
    }

    /**
     * 券预校验与抵扣计算（下单事务内，spec §5.3 第 1-2 步；扣券在订单落库后执行）。
     */
    private CouponApply resolveCoupon(Long userId, Long couponId, List<Line> lines,
                                      MemberLevel level, BigDecimal baseTotal, BigDecimal memberTotal) {
        if (couponId == null) {
            return new CouponApply(null, null, BigDecimal.ZERO, false);
        }
        UserCoupon uc = userCouponMapper.selectById(couponId);
        if (uc == null || !Objects.equals(uc.getUserId(), userId)) {
            throw new BizException(ResultCode.COUPON_INVALID, "优惠券不存在");
        }
        CouponTemplate tpl = couponTemplateMapper.selectById(uc.getTemplateId());
        if (tpl == null) {
            throw new BizException(ResultCode.COUPON_INVALID, "券模板已失效");
        }
        if (!Objects.equals(uc.getStatus(), 0)) {
            throw new BizException(ResultCode.COUPON_USED);
        }
        if (uc.getExpireAt() != null && uc.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.COUPON_EXPIRED);
        }
        CouponOptionVO opt = evaluateCoupon(uc, tpl, lines, level);
        if (!opt.isApplicable()) {
            throw new BizException(ResultCode.COUPON_INVALID, opt.getReason());
        }
        // 二选一自动取优：券方案（原价−券额）不优于会员价时不生效
        if (baseTotal.subtract(opt.getDiscount()).compareTo(memberTotal) <= 0) {
            return new CouponApply(uc, tpl, opt.getDiscount(), true);
        }
        return new CouponApply(uc, tpl, BigDecimal.ZERO, false);
    }

    /**
     * 券可用性判定（spec §5.2）：门槛只认适用商品（分类∪商品并集），allow_stack=0 时命中会员折扣不可用。
     */
    private CouponOptionVO evaluateCoupon(UserCoupon uc, CouponTemplate tpl, List<Line> lines, MemberLevel level) {
        CouponOptionVO opt = new CouponOptionVO();
        opt.setUserCouponId(uc.getId());
        if (tpl == null) {
            opt.setApplicable(false);
            opt.setReason("券模板已失效");
            return opt;
        }
        opt.setName(tpl.getName());
        opt.setMinAmount(tpl.getMinAmount());
        opt.setDiscountAmount(tpl.getDiscountAmount());
        opt.setScope(tpl.getScope());

        List<Long> catIds = CouponUtils.parseLongs(tpl.getCategoryIds());
        List<Long> prodIds = CouponUtils.parseLongs(tpl.getProductIds());
        List<Line> applicableLines = "ALL".equals(tpl.getScope()) ? lines : lines.stream()
                .filter(l -> catIds.contains(l.product.getCategoryId()) || prodIds.contains(l.product.getId()))
                .toList();
        // 用券方案按标准售价结算：门槛与抵扣以「原价小计」为准（会员价优惠在二选一自动取优中比较）
        BigDecimal subtotal = applicableLines.stream()
                .map(l -> basePrice(l).multiply(BigDecimal.valueOf(l.quantity)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            opt.setApplicable(false);
            opt.setReason("订单中没有该券适用的商品");
            return opt;
        }
        BigDecimal min = tpl.getMinAmount() == null ? BigDecimal.ZERO : tpl.getMinAmount();
        if (subtotal.compareTo(min) < 0) {
            opt.setApplicable(false);
            opt.setReason("还差 ¥" + min.subtract(subtotal).setScale(2, RoundingMode.HALF_UP) + " 可用");
            return opt;
        }
        BigDecimal discount = tpl.getDiscountAmount().min(subtotal);
        opt.setApplicable(true);
        opt.setDiscount(discount);
        return opt;
    }

    /** 行标准售价：SKU 价优先，无规格回退商品价（原价口径，ch11 自动取优用） */
    private BigDecimal basePrice(Line line) {
        return line.sku != null ? line.sku.getPrice() : line.product.getPrice();
    }

    /** 结算行 → 明细 VO（会员价口径） */
    private OrderItemVO toLineVO(Line line, MemberLevel level) {
        OrderItemVO vo = new OrderItemVO();
        vo.setProductId(line.product.getId());
        vo.setProductName(line.product.getName());
        vo.setProductImage(line.product.getMainImage());
        if (line.sku != null) {
            vo.setSpecText(SkuJsonUtils.buildSpecText(line.sku,
                    SkuJsonUtils.parseSpecs(line.product.getSpecs())));
        }
        BigDecimal unit = unitPrice(level, line);
        vo.setPrice(unit);
        vo.setQuantity(line.quantity);
        vo.setSubtotal(unit.multiply(BigDecimal.valueOf(line.quantity)));
        return vo;
    }

    /** 订单券快照（订单列表/详情展示优惠条目） */
    private void fillCoupon(OrderVO vo, Order order) {
        OrderCoupon oc = orderCouponMapper.selectOne(new LambdaQueryWrapper<OrderCoupon>()
                .eq(OrderCoupon::getOrderId, order.getId()));
        if (oc != null) {
            OrderCouponVO c = new OrderCouponVO();
            c.setId(oc.getId());
            c.setTemplateName(oc.getTemplateName());
            c.setDiscountAmount(oc.getDiscountAmount());
            vo.setCoupon(c);
        }
    }

    /**
     * 回退券（取消/超时，spec §5.4）：幂等——按 used_order_id + status=1 更新，重复执行 0 行；
     * 保留原 expire_at，已过期则置 status=2。与库存回补同事务。
     */
    private void refundCoupon(Order order) {
        userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getUsedOrderId, order.getId())
                .eq(UserCoupon::getStatus, 1)
                .setSql("status = IF(expire_at IS NOT NULL AND expire_at < NOW(), 2, 0)")
                .set(UserCoupon::getUsedOrderId, null)
                .set(UserCoupon::getUsedAt, null));
    }

    /** 成交单价：规格商品取 SKU 价，否则商品价；再按会员权益折算（ch09） */
    private BigDecimal unitPrice(MemberLevel level, Line line) {
        BigDecimal base = line.sku != null ? line.sku.getPrice() : line.product.getPrice();
        return memberLevelService.resolvePrice(level, line.product, base);
    }

    private List<Line> buildLines(Long userId, CreateOrderDTO dto) {
        if ("cart".equals(dto.getSource())) {
            List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getChecked, 1));
            if (items.isEmpty()) {
                throw new BizException(ResultCode.PARAM_ERROR, "请先勾选要结算的商品");
            }
            Map<Long, Product> productMap = productMapper.selectBatchIds(
                            items.stream().map(CartItem::getProductId).distinct().toList())
                    .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
            List<Line> lines = new ArrayList<>();
            for (CartItem item : items) {
                Product product = productMap.get(item.getProductId());
                if (product == null || !Objects.equals(product.getStatus(), 1)) {
                    throw new BizException(ResultCode.PARAM_ERROR, "「" + (product == null ? "商品" : product.getName()) + "」已下架或不存在");
                }
                List<SkuVO> skus = SkuJsonUtils.parseSkus(product.getSkus());
                SkuVO sku = SkuJsonUtils.findSku(skus, item.getSkuId());
                if (!skus.isEmpty() && sku == null) {
                    throw new BizException(ResultCode.PARAM_ERROR, "「" + product.getName() + "」规格已失效，请重新选购");
                }
                if (sku != null && item.getQuantity() > (sku.getStock() == null ? 0 : sku.getStock())) {
                    throw new BizException(ResultCode.PARAM_ERROR, "「" + product.getName() + "」该规格库存不足，请调整数量");
                }
                lines.add(new Line(product, sku, item.getQuantity()));
            }
            return lines;
        }

        // buyNow
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || !Objects.equals(product.getStatus(), 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "商品不存在或已下架");
        }
        List<SkuVO> skus = SkuJsonUtils.parseSkus(product.getSkus());
        SkuVO sku = SkuJsonUtils.findSku(skus, dto.getSkuId());
        if (!skus.isEmpty() && sku == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "该规格不存在");
        }
        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        int available = sku != null ? (sku.getStock() == null ? 0 : sku.getStock())
                : (product.getStock() == null ? 0 : product.getStock());
        if (quantity > Math.min(99, available)) {
            throw new BizException(ResultCode.PARAM_ERROR, "「" + product.getName() + "」库存不足（剩余 "
                    + available + " 件）");
        }
        return List.of(new Line(product, sku, quantity));
    }

    /** 越权访问一律 404「订单不存在」，不暴露存在性 */
    private Order requireOwnedOrder(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private Address requireOwnedAddress(Long userId, Long addressId) {
        Address address = addressMapper.selectById(addressId);
        if (address == null || !Objects.equals(address.getUserId(), userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "地址不存在");
        }
        return address;
    }

    /** 库存回补（取消订单） */
    private void restoreStock(Order order) {
        for (OrderItem item : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()))) {
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, item.getProductId())
                    .setSql("stock = stock + " + item.getQuantity()));
            if (item.getSkuId() != null && item.getSkuId() > 0) {
                changeSkuStock(item.getProductId(), item.getSkuId(), item.getQuantity());
            }
        }
    }

    /** SKU 显示库存增减（读改写，尽力同步；并发安全真源为 product.stock 条件更新） */
    private void changeSkuStock(Long productId, Long skuId, int delta) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return;
        }
        List<SkuVO> skus = SkuJsonUtils.parseSkus(product.getSkus());
        SkuVO target = SkuJsonUtils.findSku(skus, skuId);
        if (target == null) {
            return;
        }
        int stock = target.getStock() == null ? 0 : target.getStock();
        target.setStock(Math.max(0, stock + delta));
        String json = SkuJsonUtils.writeSkus(skus);
        if (json != null) {
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, productId)
                    .set(Product::getSkus, json));
        }
    }

    /** 业务唯一号生成：前缀 + 时间戳 + 8 位随机数字；订单号查重、支付流水号查重，冲突重试 ≤3 次 */
    private String generateNo(String prefix) {
        for (int i = 0; i < 3; i++) {
            String no = prefix + LocalDateTime.now().format(NO_FORMAT)
                    + String.format("%08d", ThreadLocalRandom.current().nextInt(100000000));
            if ("P".equals(prefix)) {
                Long count = paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPaymentNo, no));
                if (count == null || count == 0) {
                    return no;
                }
            } else {
                Long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, no));
                if (count == null || count == 0) {
                    return no;
                }
            }
        }
        throw new BizException(ResultCode.ERROR, "流水号生成失败，请重试");
    }

    private String fullAddress(Address address) {
        StringBuilder sb = new StringBuilder(address.getProvince())
                .append(address.getCity());
        if (address.getDistrict() != null) {
            sb.append(address.getDistrict());
        }
        sb.append(address.getDetail());
        return sb.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private OrderVO toVo(Order order) {
        OrderVO vo = new OrderVO();
        fillBase(vo, order);
        vo.setItems(fetchItems(order.getId()));
        return vo;
    }

    private AdminOrderVO toAdminVo(Order order, String userName) {
        AdminOrderVO vo = new AdminOrderVO();
        fillBase(vo, order);
        vo.setUserId(order.getUserId());
        vo.setUserName(userName);
        vo.setItems(fetchItems(order.getId()));
        return vo;
    }

    private void fillBase(OrderVO vo, Order order) {
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        fillCoupon(vo, order);
        vo.setRemark(order.getRemark());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddr(order.getReceiverAddr());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCreateTime(order.getCreateTime());
        // 支付截止 = 订单创建 + 15 分钟（仅待支付返回；其余状态为 null）
        if (Objects.equals(order.getStatus(), 0) && order.getCreateTime() != null) {
            vo.setPayDeadline(order.getCreateTime().plusMinutes(PAY_TIMEOUT_MINUTES));
        }
    }

    private List<OrderItemVO> fetchItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId))
                .stream().map(item -> {
                    OrderItemVO vo = new OrderItemVO();
                    vo.setId(item.getId());
                    vo.setProductId(item.getProductId());
                    vo.setSpecText(item.getSpecText());
                    vo.setProductName(item.getProductName());
                    vo.setProductImage(item.getProductImage());
                    vo.setPrice(item.getPrice());
                    vo.setQuantity(item.getQuantity());
                    vo.setSubtotal(item.getSubtotal());
                    return vo;
                })
                .toList();
    }

    private String statusText(Integer status) {
        if (status == null || status < 0 || status >= STATUS_TEXT.length) {
            return "未知";
        }
        return STATUS_TEXT[status];
    }

    private Map<Long, String> fetchUserNames(List<Order> orders) {
        List<Long> userIds = orders.stream().map(Order::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }
}
