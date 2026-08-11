package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.dto.ProductDTO;
import com.dyshop.api.mapper.CartItemMapper;
import com.dyshop.api.mapper.CategoryMapper;
import com.dyshop.api.mapper.FavoriteMapper;
import com.dyshop.api.mapper.OrderItemMapper;
import com.dyshop.api.mapper.ProductMapper;
import com.dyshop.api.util.SkuJsonUtils;
import com.dyshop.api.vo.AdminProductVO;
import com.dyshop.api.vo.SkuVO;
import com.dyshop.common.entity.CartItem;
import com.dyshop.common.entity.Category;
import com.dyshop.common.entity.Favorite;
import com.dyshop.common.entity.OrderItem;
import com.dyshop.common.entity.Product;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台商品管理实现。
 * <p>
 * 规格/库存规则（docs/ch08/spec.md D4）：
 * 有规格商品保存时 product.stock 自动 = Σ sku.stock（product.stock 为下单扣减真源，
 * sku.stock 为尽力同步展示口径，防止两边不一致导致误判缺货）。
 */
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final CartItemMapper cartItemMapper;
    private final FavoriteMapper favoriteMapper;
    private final OrderItemMapper orderItemMapper;

    public IPage<AdminProductVO> list(String keyword, Long categoryId, Integer status, long page, long size) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<Product>()
                .like(keyword != null && !keyword.isBlank(), Product::getName, keyword)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .eq(status != null, Product::getStatus, status)
                .orderByDesc(Product::getId);
        IPage<Product> p = productMapper.selectPage(new Page<>(page, size), qw);
        if (p.getRecords().isEmpty()) {
            return new Page<>(page, size);
        }
        Map<Long, Category> categoryMap = categoryMapper.selectBatchIds(
                        p.getRecords().stream().map(Product::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        List<AdminProductVO> vos = p.getRecords().stream()
                .map(pr -> toVo(pr, categoryMap.get(pr.getCategoryId())))
                .toList();
        Page<AdminProductVO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(vos);
        return result;
    }

    public AdminProductVO get(Long id) {
        Product product = requireProduct(id);
        Category category = categoryMapper.selectById(product.getCategoryId());
        return toVo(product, category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(ProductDTO dto) {
        productMapper.insert(buildProduct(new Product(), dto));
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductDTO dto) {
        Product product = requireProduct(id);
        productMapper.updateById(buildProduct(product, dto));
    }

    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态参数错误");
        }
        Product product = requireProduct(id);
        product.setStatus(status);
        productMapper.updateById(product);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        requireProduct(id);
        // 引用检查：购物车 / 收藏 / 订单明细任一存在引用 → 拒绝删除，引导改为下架
        long cartRef = cartItemMapper.selectCount(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getProductId, id));
        long favRef = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getProductId, id));
        long orderRef = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getProductId, id));
        if (cartRef > 0 || favRef > 0 || orderRef > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "该商品存在交易/收藏引用，请改为下架");
        }
        // @TableLogic：逻辑删除，C 端列表/详情自动过滤
        productMapper.deleteById(id);
    }

    // ---------- 私有 ----------

    private Product requireProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        return product;
    }

    /** 组装实体：校验分类/规格 JSON；有规格时总库存 = Σ sku.stock */
    private Product buildProduct(Product product, ProductDTO dto) {
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null || !Objects.equals(category.getStatus(), 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "所选分类不存在或已停用");
        }

        boolean hasSpecs = dto.getSpecs() != null && !dto.getSpecs().isBlank();
        boolean hasSkus = dto.getSkus() != null && !dto.getSkus().isBlank();
        if (hasSpecs != hasSkus) {
            throw new BizException(ResultCode.PARAM_ERROR, "规格数据格式错误：specs 与 skus 必须同时填写或同时留空");
        }

        Integer stock = dto.getStock();
        if (hasSpecs) {
            // 严格解析：损坏 JSON / 结构不符 → 400（不信任前端直传）
            List<SkuVO> skus = SkuJsonUtils.parseSkus(dto.getSkus());
            if (skus.isEmpty()) {
                throw new BizException(ResultCode.PARAM_ERROR, "规格数据格式错误");
            }
            if (SkuJsonUtils.parseSpecs(dto.getSpecs()).isEmpty()) {
                throw new BizException(ResultCode.PARAM_ERROR, "规格数据格式错误");
            }
            // SKU 库存汇总：总库存自动同步，防止两口径不一致
            stock = skus.stream()
                    .map(s -> s.getStock() == null ? 0 : s.getStock())
                    .reduce(0, Integer::sum);
            product.setSpecs(dto.getSpecs());
            product.setSkus(dto.getSkus());
        } else {
            product.setSpecs(null);
            product.setSkus(null);
        }

        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName().trim());
        product.setSubtitle(trimToNull(dto.getSubtitle()));
        product.setMainImage(dto.getMainImage().trim());
        product.setImages(trimToNull(dto.getImages()));
        product.setDetail(trimToNull(dto.getDetail()));
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setVipPrice(dto.getVipPrice());
        product.setStock(stock == null ? 0 : stock);
        product.setStatus(dto.getStatus());
        return product;
    }

    private AdminProductVO toVo(Product product, Category category) {
        AdminProductVO vo = new AdminProductVO();
        vo.setId(product.getId());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setMainImage(product.getMainImage());
        vo.setImages(product.getImages());
        vo.setDetail(product.getDetail());
        vo.setSpecs(product.getSpecs());
        vo.setSkus(product.getSkus());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setVipPrice(product.getVipPrice());
        vo.setStock(product.getStock());
        vo.setSales(product.getSales());
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}