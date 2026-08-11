package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dyshop.api.mapper.CartItemMapper;
import com.dyshop.api.mapper.ProductMapper;
import com.dyshop.api.util.SkuJsonUtils;
import com.dyshop.api.vo.CartItemVO;
import com.dyshop.api.vo.SkuVO;
import com.dyshop.common.entity.CartItem;
import com.dyshop.common.entity.Product;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl {

    /** 单件商品最大数量 */
    private static final int MAX_QUANTITY = 99;

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    public List<CartItemVO> listCart(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getCreateTime));

        List<Long> productIds = items.stream()
                .map(CartItem::getProductId).distinct().toList();
        Map<Long, Product> productMap = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));

        return items.stream()
                // 商品已删除/不存在的条目跳过（无数据可展示）；下架（status=0）商品保留，
                // 前端置灰展示并标记 productStatus=0，不可勾选结算
                .map(it -> {
                    Product product = productMap.get(it.getProductId());
                    if (product == null) {
                        return null;
                    }
                    // 规格商品：行价格/库存按 SKU 取；无规格商品回退商品级
                    SkuVO sku = SkuJsonUtils.findSku(
                            SkuJsonUtils.parseSkus(product.getSkus()), it.getSkuId());
                    CartItemVO vo = new CartItemVO();
                    vo.setCartItemId(it.getId());
                    vo.setProductId(product.getId());
                    vo.setSkuId(it.getSkuId());
                    vo.setSpecText(it.getSpecText());
                    vo.setName(product.getName());
                    vo.setSubtitle(product.getSubtitle());
                    vo.setMainImage(product.getMainImage());
                    vo.setPrice(sku != null ? sku.getPrice() : product.getPrice());
                    vo.setOriginalPrice(sku != null && sku.getOriginalPrice() != null
                            ? sku.getOriginalPrice() : product.getOriginalPrice());
                    vo.setStock(sku != null ? sku.getStock() : product.getStock());
                    vo.setProductStatus(product.getStatus());
                    vo.setSales(product.getSales());
                    vo.setQuantity(it.getQuantity());
                    vo.setChecked(it.getChecked());
                    return vo;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public void addItem(Long userId, Long productId, Long skuId, int quantity) {
        Product product = requireOnSaleProduct(productId);

        List<SkuVO> skus = SkuJsonUtils.parseSkus(product.getSkus());
        SkuVO sku = SkuJsonUtils.findSku(skus, skuId);
        // 规格商品：skuId 必须有效且在售；specText 服务端按 SKU 生成（不信任前端直传）
        String specText = null;
        if (!skus.isEmpty()) {
            if (sku == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "该规格不存在");
            }
            if (sku.getStock() == null || sku.getStock() <= 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "该规格已售罄");
            }
            specText = SkuJsonUtils.buildSpecText(sku, SkuJsonUtils.parseSpecs(product.getSpecs()));
        }

        long skuKey = skuId == null ? 0L : skuId;
        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId)
                .eq(CartItem::getSkuId, skuKey));
        int target = quantity;
        if (existing != null) {
            target = existing.getQuantity() + quantity;
        }
        checkStockLimit(product, sku, target);

        if (existing == null) {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setSkuId(skuKey);
            item.setSpecText(specText);
            item.setQuantity(target);
            item.setChecked(1);
            cartItemMapper.insert(item);
        } else {
            existing.setQuantity(target);
            cartItemMapper.updateById(existing);
        }
    }

    public void updateQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem existing = requireOwnedItem(userId, cartItemId);
        Product product = requireOnSaleProduct(existing.getProductId());
        SkuVO sku = SkuJsonUtils.findSku(
                SkuJsonUtils.parseSkus(product.getSkus()), existing.getSkuId());
        checkStockLimit(product, sku, quantity);

        existing.setQuantity(quantity);
        cartItemMapper.updateById(existing);
    }

    public void removeItem(Long userId, Long cartItemId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getId, cartItemId)
                .eq(CartItem::getUserId, userId));
    }

    public void updateChecked(Long userId, Long cartItemId, int checked) {
        CartItem existing = requireOwnedItem(userId, cartItemId);
        existing.setChecked(checked);
        cartItemMapper.updateById(existing);
    }

    public void clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
    }

    private Product requireOnSaleProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || !Objects.equals(product.getStatus(), 1)) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        return product;
    }

    /** 按行 id + 归属定位购物车条目 */
    private CartItem requireOwnedItem(Long userId, Long cartItemId) {
        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getId, cartItemId)
                .eq(CartItem::getUserId, userId));
        if (item == null) {
            throw new BizException(ResultCode.NOT_FOUND, "购物车条目不存在");
        }
        return item;
    }

    private void checkStockLimit(Product product, SkuVO sku, int quantity) {
        // 规格商品以 SKU 库存校验；无规格商品回退 product.stock
        int max = Math.min(MAX_QUANTITY, (sku != null ? sku.getStock() : product.getStock()) == null ? 0
                : (sku != null ? sku.getStock() : product.getStock()));
        if (quantity > max) {
            if (max <= 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "该商品已售罄");
            }
            throw new BizException(ResultCode.PARAM_ERROR, "库存不足（剩余 " + max + " 件）");
        }
    }
}