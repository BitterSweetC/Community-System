package com.cloud.community.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 在每个响应上注入安全相关 HTTP 头，防范常见 Web 攻击：
 * - X-Frame-Options：防止点击劫持
 * - X-Content-Type-Options：防止 MIME 类型嗅探
 * - X-XSS-Protection：旧版浏览器 XSS 过滤
 * - Referrer-Policy：控制 Referer 信息泄露
 * - Permissions-Policy：关闭不需要的浏览器功能
 * - Strict-Transport-Security：仅在 HTTPS 启用后生效
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // HSTS 仅在 HTTPS 环境下启用，HTTP 下设置无效且会破坏访问
        if (sslEnabled) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        filterChain.doFilter(request, response);
    }
}
