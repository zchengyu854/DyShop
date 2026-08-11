package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.mapper.FavoriteMapper;
import com.dyshop.api.mapper.ProductMapper;
import com.dyshop.api.vo.FavoriteVO;
import com.dyshop.common.entity.Favorite;
import com.dyshop.common.entity.Product;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.PageResult;
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
public class FavoriteServiceImpl {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;

    public PageResult<FavoriteVO> pageFavorites(Long userId, int page, int size) {
        Page<Favorite> p = new Page<>(page, size);
        favoriteMapper.selectPage(p, new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime));

        List<Long> productIds = p.getRecords().stream()
                .map(Favorite::getProductId).distinct().toList();
        Map<Long, Product> productMap = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));

        List<FavoriteVO> records = p.getRecords().stream().map(f -> {
            FavoriteVO vo = new FavoriteVO();
            vo.setFavoriteId(f.getId());
            vo.setCreateTime(f.getCreateTime());
            Product product = productMap.get(f.getProductId());
            if (product != null) {
                vo.setProductId(product.getId());
                vo.setName(product.getName());
                vo.setSubtitle(product.getSubtitle());
                vo.setMainImage(product.getMainImage());
                vo.setPrice(product.getPrice());
                vo.setOriginalPrice(product.getOriginalPrice());
                vo.setSales(product.getSales());
            }
            return vo;
        }).toList();
        return PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    public void addFavorite(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || !Objects.equals(product.getStatus(), 1)) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (count != null && count > 0) {
            return; // 幂等：已收藏不报错
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favoriteMapper.insert(favorite);
    }

    public void removeFavorite(Long userId, Long productId) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
    }

    public boolean hasFavorited(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || !Objects.equals(product.getStatus(), 1)) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        return count != null && count > 0;
    }
}