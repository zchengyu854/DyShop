package com.dyshop.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 交易类接口统一禁用 HTTP 缓存。
 * <p>
 * 故障根因（配合前端排查）：GET /orders、/orders/{id} 等接口若被浏览器、
 * 网关/CDN 启发式缓存，下单/支付成功回调后用户返回列表仍命中缓存，展示旧数据。
 * <p>
 * 修复：对订单与商品动态接口强制返回 no-store。前端 request 层亦显式携带
 * Cache-Control: no-store 请求头（双保险，覆盖代理缓存场景）。
 */
@Component
public class CacheControlFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/orders") || path.startsWith("/api/admin/orders")
                || path.startsWith("/api/admin/stats")
                || path.startsWith("/api/products") || path.startsWith("/api/cart")
                || path.startsWith("/api/user/overview") || path.startsWith("/api/user/dashboard-stats")) {
            response.setHeader("Cache-Control", "no-store, no-cache, max-age=0, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        filterChain.doFilter(request, response);
    }
}