package com.dyshop.api.controller.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dyshop.api.dto.PointsGoodsDTO;
import com.dyshop.api.service.impl.AdminPointsServiceImpl;
import com.dyshop.api.vo.PointsExchangeAdminVO;
import com.dyshop.api.vo.PointsGoodsAdminVO;
import com.dyshop.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台积分商城接口（ch13，ROLE_ADMIN，路径 /api/admin/** 已由 SecurityConfig 保护）。
 */
@RestController
@RequestMapping("/api/admin/points")
@RequiredArgsConstructor
public class AdminPointsController {

    private final AdminPointsServiceImpl adminPointsService;

    /** 商品分页/搜索 */
    @GetMapping("/goods")
    public Result<IPage<PointsGoodsAdminVO>> goods(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer status) {
        return Result.success(adminPointsService.pageGoods(page, size, keyword, status));
    }

    /** 新建商品 */
    @PostMapping("/goods")
    public Result<PointsGoodsAdminVO> create(@Valid @RequestBody PointsGoodsDTO dto) {
        return Result.success(adminPointsService.create(dto));
    }

    /** 编辑商品 */
    @PutMapping("/goods/{id}")
    public Result<PointsGoodsAdminVO> update(@PathVariable Long id, @Valid @RequestBody PointsGoodsDTO dto) {
        return Result.success(adminPointsService.update(id, dto));
    }

    /** 上/下架 */
    @PatchMapping("/goods/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminPointsService.updateStatus(id, status);
        return Result.success();
    }

    /** 逻辑删除（仅下架商品） */
    @DeleteMapping("/goods/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminPointsService.delete(id);
        return Result.success();
    }

    /** 兑换记录分页查询 */
    @GetMapping("/exchanges")
    public Result<IPage<PointsExchangeAdminVO>> exchanges(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) Long goodsId,
                                                          @RequestParam(required = false) String keyword) {
        return Result.success(adminPointsService.pageExchanges(page, size, goodsId, keyword));
    }
}