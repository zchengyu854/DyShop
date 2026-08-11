package com.dyshop.api.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dyshop.api.service.impl.AdminUserServiceImpl;
import com.dyshop.api.vo.AdminUserVO;
import com.dyshop.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台用户管理接口（需 ROLE_ADMIN）。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserServiceImpl adminUserService;

    @GetMapping
    public Result<IPage<AdminUserVO>> list(@RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminUserService.list(keyword, page, size));
    }

    @GetMapping("/me")
    public Result<AdminUserVO> me() {
        return Result.success(adminUserService.me(currentUserId()));
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminUserService.changeStatus(id, status, currentUserId());
        return Result.success();
    }

    @PutMapping("/{id}/role")
    public Result<Void> changeRole(@PathVariable Long id, @RequestParam Integer role) {
        adminUserService.changeRole(id, role, currentUserId());
        return Result.success();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}