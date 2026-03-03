package com.cloud.community.gateway.controller;

import com.cloud.community.core.annotation.AuditLog;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.User;
import com.cloud.community.gateway.security.JwtUtils;
import com.cloud.community.core.service.VerificationCodeService;
import com.cloud.community.user.service.UserService;
import com.cloud.community.gateway.metrics.MetricsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final VerificationCodeService verificationCodeService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final MetricsService metricsService;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:86400000}")
    private long refreshExpiration;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    // ── 限频常量 ──────────────────────────────────────────────────────────────
    private static final int    MAX_LOGIN_ATTEMPTS = 10;
    private static final long   ATTEMPT_WINDOW_MIN = 15;
    private static final String ATTEMPT_KEY_PREFIX = "login:attempts:";
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";

    // ── 密码强度正则：至少 8 位，含字母和数字 ─────────────────────────────────
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        validatePasswordStrength(user.getPassword());
        return Result.success(userService.register(user));
    }

    @AuditLog(action = "LOGIN", userId = "#result.data.user.id")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request,
                                       HttpServletResponse response) {
        checkLoginRateLimit(request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 登录成功：清除失败计数
        redisTemplate.delete(ATTEMPT_KEY_PREFIX + request.getUsername());
        metricsService.recordLoginSuccess();

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Access token
        String accessToken = jwtUtils.generateToken(userDetails);
        redisTemplate.opsForValue().set("auth:token:" + accessToken,
                userDetails.getUsername(), jwtExpiration, TimeUnit.MILLISECONDS);
        setAccessTokenCookie(response, accessToken);

        // Refresh token
        String newRefreshToken = jwtUtils.generateRefreshToken(userDetails);
        redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + userDetails.getUsername(),
                newRefreshToken, refreshExpiration, TimeUnit.MILLISECONDS);
        setRefreshTokenCookie(response, newRefreshToken);

        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return Result.success(new LoginResponse(user));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(HttpServletRequest request,
                                         HttpServletResponse response) {
        String refreshToken = extractCookieValue(request, "refresh_token");
        if (refreshToken == null) {
            throw new IllegalArgumentException("未找到刷新令牌，请重新登录");
        }

        String username = jwtUtils.extractUsername(refreshToken);

        // 校验 Redis 中存储的 refresh token
        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + username);
        if (!refreshToken.equals(stored) || !jwtUtils.isRefreshTokenValid(refreshToken, username)) {
            throw new IllegalArgumentException("刷新令牌已失效，请重新登录");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // 生成新 access token
        String newAccessToken = jwtUtils.generateToken(userDetails);
        redisTemplate.opsForValue().set("auth:token:" + newAccessToken,
                username, jwtExpiration, TimeUnit.MILLISECONDS);
        setAccessTokenCookie(response, newAccessToken);

        // 轮换 refresh token（旧的失效，写入新的）
        String rotatedRefresh = jwtUtils.generateRefreshToken(userDetails);
        redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + username,
                rotatedRefresh, refreshExpiration, TimeUnit.MILLISECONDS);
        setRefreshTokenCookie(response, rotatedRefresh);

        User user = userService.findByUsername(username).orElseThrow();
        return Result.success(new LoginResponse(user));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractCookieValue(request, "access_token");
        if (accessToken != null) {
            redisTemplate.delete("auth:token:" + accessToken);
        }
        String username = null;
        try {
            if (accessToken != null) username = jwtUtils.extractUsername(accessToken);
        } catch (Exception ignored) {}
        if (username != null) {
            redisTemplate.delete(REFRESH_KEY_PREFIX + username);
        }

        clearCookie(response, "access_token");
        clearCookie(response, "refresh_token");
        return Result.success();
    }

    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("该邮箱未注册"));
        verificationCodeService.sendVerificationCode(user.getEmail());
        return Result.success();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        validatePasswordStrength(request.getNewPassword());

        boolean verified = verificationCodeService.verifyCode(request.getEmail(), request.getCode());
        if (!verified) {
            throw new IllegalArgumentException("验证码无效或已过期");
        }

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        userService.updatePassword(user.getId(), request.getNewPassword());
        return Result.success();
    }

    // ── Cookie 辅助 ───────────────────────────────────────────────────────────

    private void setAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(Duration.ofMillis(jwtExpiration))
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(Duration.ofMillis(refreshExpiration))
                .path("/api/auth/refresh")   // 仅刷新端点需要携带
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie expired = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
    }

    private String extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (name.equals(c.getName())) return c.getValue();
            }
        }
        return null;
    }

    // ── 限频 & 密码校验 ───────────────────────────────────────────────────────

    private void checkLoginRateLimit(String username) {
        String key = ATTEMPT_KEY_PREFIX + username;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts == 1) {
            redisTemplate.expire(key, ATTEMPT_WINDOW_MIN, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts > MAX_LOGIN_ATTEMPTS) {
            long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
            metricsService.recordLoginFailure();
            throw new IllegalArgumentException("登录失败次数过多，请 " + ttl + " 分钟后再试");
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !password.matches(PASSWORD_PATTERN)) {
            throw new IllegalArgumentException("密码至少8位，且必须同时包含字母和数字");
        }
    }

    // ── 内部 DTO ──────────────────────────────────────────────────────────────

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private User user;

        public LoginResponse(User user) {
            this.user = user;
        }
    }

    @Data
    public static class ForgotPasswordRequest {
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        private String email;
        private String code;
        private String newPassword;
    }
}
