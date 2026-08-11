package com.dyshop.api.controller.user;

import com.dyshop.api.dto.UpdatePasswordDTO;
import com.dyshop.api.dto.UpdateProfileDTO;
import com.dyshop.api.service.impl.FavoriteServiceImpl;
import com.dyshop.api.service.impl.MemberLevelServiceImpl;
import com.dyshop.api.service.impl.OrderServiceImpl;
import com.dyshop.api.service.impl.UserCenterServiceImpl;
import com.dyshop.api.vo.DashboardStatsVO;
import com.dyshop.api.vo.FavoriteStatusVO;
import com.dyshop.api.vo.FavoriteVO;
import com.dyshop.api.vo.MemberOverviewVO;
import com.dyshop.api.vo.MemberPricePreviewVO;
import com.dyshop.api.vo.PointLogVO;
import com.dyshop.api.vo.UserOrderOverviewVO;
import com.dyshop.api.vo.UserVO;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.PageResult;
import com.dyshop.common.result.Result;
import com.dyshop.common.result.ResultCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心接口（全部需认证，principal=userId）。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserCenterServiceImpl userCenterService;
    private final FavoriteServiceImpl favoriteService;
    private final OrderServiceImpl orderService;
    private final MemberLevelServiceImpl memberLevelService;

    @GetMapping("/overview")
    public Result<UserOrderOverviewVO> overview() {
        return Result.success(orderService.overview(currentUserId()));
    }

    /** 个人中心首页聚合统计：一次请求返回订单概览 + 会员全景，避免前端多次并发 */
    @GetMapping("/dashboard-stats")
    public Result<DashboardStatsVO> dashboardStats() {
        Long userId = currentUserId();
        UserOrderOverviewVO order = orderService.overview(userId);
        MemberOverviewVO member = memberLevelService.overview(userId);

        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setTotalSpent(order.getTotalConsumption());
        vo.setTotalOrders(order.getTotalOrders());
        vo.setPendingShipment(order.getWaitShip());
        vo.setPendingReceive(order.getWaitReceive());
        vo.setPoints(member.getPoints());
        vo.setLevelCode(member.getLevel() == null ? null : member.getLevel().getCode());
        vo.setLevelName(member.getLevel() == null ? null : member.getLevel().getName());
        vo.setNextLevelThreshold(member.getNextLevel() == null ? null : member.getNextLevel().getThreshold());
        vo.setNeedAmount(member.getNeedAmount());
        vo.setProgressPct(member.getProgressPct());
        return Result.success(vo);
    }

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userCenterService.getProfile(currentUserId()));
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success(userCenterService.updateProfile(currentUserId(), dto));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        userCenterService.updatePassword(currentUserId(), dto);
        return Result.success();
    }

    @GetMapping("/favorites")
    public Result<PageResult<FavoriteVO>> favorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BizException(ResultCode.PARAM_ERROR, "size 必须在 1~" + MAX_PAGE_SIZE + " 之间");
        }
        return Result.success(favoriteService.pageFavorites(currentUserId(), page, size));
    }

    @PostMapping("/favorites/{productId}")
    public Result<Void> addFavorite(@PathVariable Long productId) {
        favoriteService.addFavorite(currentUserId(), productId);
        return Result.success();
    }

    @DeleteMapping("/favorites/{productId}")
    public Result<Void> removeFavorite(@PathVariable Long productId) {
        favoriteService.removeFavorite(currentUserId(), productId);
        return Result.success();
    }

    @GetMapping("/favorites/status/{productId}")
    public Result<FavoriteStatusVO> favoriteStatus(@PathVariable Long productId) {
        FavoriteStatusVO vo = new FavoriteStatusVO();
        vo.setFavorited(favoriteService.hasFavorited(currentUserId(), productId));
        return Result.success(vo);
    }

    @GetMapping("/member/overview")
    public Result<MemberOverviewVO> memberOverview() {
        return Result.success(memberLevelService.overview(currentUserId()));
    }

    /** 结算价格预览：body = { rows: [{productId, skuId}] }（数量不影响单价口径） */
    @PostMapping("/member/price-preview")
    public Result<MemberPricePreviewVO> memberPricePreview(@RequestBody MemberPricePreviewVO request) {
        var rows = request.getRows();
        if (rows == null) {
            rows = java.util.List.of();
        }
        return Result.success(memberLevelService.previewPrices(currentUserId(), rows));
    }

    @GetMapping("/member/points")
    public Result<PageResult<PointLogVO>> memberPoints(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BizException(ResultCode.PARAM_ERROR, "size 必须在 1~" + MAX_PAGE_SIZE + " 之间");
        }
        return Result.success(memberLevelService.pointPage(currentUserId(), page, size));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}