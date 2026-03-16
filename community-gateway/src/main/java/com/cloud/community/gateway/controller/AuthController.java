package com.cloud.community.gateway.controller;

import com.cloud.community.core.annotation.AuditLog;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.metrics.BusinessMetricsService;
import com.cloud.community.core.service.VerificationCodeService;
import com.cloud.community.gateway.security.JwtUtils;
import com.cloud.community.user.service.UserService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final long ATTEMPT_WINDOW_MINUTES = 15;
    private static final String ATTEMPT_KEY_PREFIX = "login:attempts:";
    private static final String ACCESS_KEY_PREFIX = "auth:token:";
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final VerificationCodeService verificationCodeService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final BusinessMetricsService metricsService;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:86400000}")
    private long refreshExpiration;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        validatePasswordStrength(user.getPassword());
        return Result.success(userService.register(user));
    }

    @AuditLog(action = "LOGIN", userId = "#result.data.user.id")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        checkLoginRateLimit(request.getUsername());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (RuntimeException ex) {
            metricsService.recordLoginFailure();
            throw ex;
        }

        redisTemplate.delete(ATTEMPT_KEY_PREFIX + request.getUsername());
        metricsService.recordLoginSuccess();

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtUtils.generateToken(userDetails);
        redisTemplate.opsForValue().set(ACCESS_KEY_PREFIX + accessToken,
                userDetails.getUsername(), jwtExpiration, TimeUnit.MILLISECONDS);
        setAccessTokenCookie(response, accessToken);

        String refreshToken = jwtUtils.generateRefreshToken(userDetails);
        redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + userDetails.getUsername(),
                refreshToken, refreshExpiration, TimeUnit.MILLISECONDS);
        setRefreshTokenCookie(response, refreshToken);

        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return Result.success(new LoginResponse(user));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookieValue(request, "refresh_token");
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token not found. Please log in again.");
        }

        String username = jwtUtils.extractUsername(refreshToken);
        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + username);
        if (!refreshToken.equals(stored) || !jwtUtils.isRefreshTokenValid(refreshToken, username)) {
            throw new IllegalArgumentException("Refresh token is invalid or expired.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!userDetails.isEnabled()) {
            redisTemplate.delete(REFRESH_KEY_PREFIX + username);
            clearCookie(response, "access_token", "/");
            clearCookie(response, "refresh_token", "/");
            clearCookie(response, "refresh_token", "/api/auth/refresh");
            throw new IllegalArgumentException("User account is disabled");
        }

        String newAccessToken = jwtUtils.generateToken(userDetails);
        redisTemplate.opsForValue().set(ACCESS_KEY_PREFIX + newAccessToken,
                username, jwtExpiration, TimeUnit.MILLISECONDS);
        setAccessTokenCookie(response, newAccessToken);

        String rotatedRefreshToken = jwtUtils.generateRefreshToken(userDetails);
        redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + username,
                rotatedRefreshToken, refreshExpiration, TimeUnit.MILLISECONDS);
        setRefreshTokenCookie(response, rotatedRefreshToken);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return Result.success(new LoginResponse(user));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractCookieValue(request, "access_token");
        if (accessToken != null) {
            redisTemplate.delete(ACCESS_KEY_PREFIX + accessToken);
        }

        String refreshToken = extractCookieValue(request, "refresh_token");
        if (refreshToken != null) {
            try {
                String refreshUsername = jwtUtils.extractUsername(refreshToken);
                if (refreshUsername != null) {
                    redisTemplate.delete(REFRESH_KEY_PREFIX + refreshUsername);
                }
            } catch (Exception ignored) {
            }
        }

        String username = null;
        try {
            if (accessToken != null) {
                username = jwtUtils.extractUsername(accessToken);
            }
        } catch (Exception ignored) {
        }
        if (username != null) {
            redisTemplate.delete(REFRESH_KEY_PREFIX + username);
        }

        clearCookie(response, "access_token", "/");
        clearCookie(response, "refresh_token", "/");
        clearCookie(response, "refresh_token", "/api/auth/refresh");
        return Result.success();
    }

    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email is not registered"));
        verificationCodeService.sendVerificationCode(user.getEmail());
        return Result.success();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        validatePasswordStrength(request.getNewPassword());

        boolean verified = verificationCodeService.verifyCode(request.getEmail(), request.getCode());
        if (!verified) {
            throw new IllegalArgumentException("Verification code is invalid or expired");
        }

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userService.updatePassword(user.getId(), request.getNewPassword());
        return Result.success();
    }

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
                .path("/api/auth/refresh")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        ResponseCookie expired = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(0)
                .path(path)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
    }

    private String extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void checkLoginRateLimit(String username) {
        String key = ATTEMPT_KEY_PREFIX + username;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts == 1) {
            redisTemplate.expire(key, ATTEMPT_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts > MAX_LOGIN_ATTEMPTS) {
            long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
            metricsService.recordLoginFailure();
            throw new IllegalArgumentException("Too many login attempts. Retry in " + ttl + " minutes.");
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !password.matches(PASSWORD_PATTERN)) {
            throw new IllegalArgumentException("Password must be at least 8 characters and include letters and digits.");
        }
    }

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