package com.dyshop.api.controller.auth;

import com.dyshop.api.dto.LoginDTO;
import com.dyshop.api.dto.RegisterDTO;
import com.dyshop.api.service.impl.AuthServiceImpl;
import com.dyshop.api.vo.LoginVO;
import com.dyshop.api.vo.UserVO;
import com.dyshop.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端认证接口：注册 / 登录 / 当前用户信息。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;

    /** 注册（成功后直接登录，返回 token） */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /** 当前登录用户信息（需认证） */
    @GetMapping("/me")
    public Result<UserVO> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(authService.me(userId));
    }
}
