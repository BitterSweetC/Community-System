package com.cloud.community.gateway.controller;

import com.cloud.community.core.annotation.AuditLog;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.User;
import com.cloud.community.gateway.security.JwtUtils;
import com.cloud.community.core.service.VerificationCodeService;
import com.cloud.community.user.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
// @CrossOrigin(origins = "*") // Handled globally in SecurityConfig
public class AuthController {

    private final UserService userService;
    private final VerificationCodeService verificationCodeService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        return Result.success(userService.register(user));
    }

    @AuditLog(action = "LOGIN", userId = "#result.data.user.id")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);
        
        // Store token in Redis with same expiration as JWT (1 hour)
        // Key: auth:token:{token} -> Value: username (or UserDetails)
        redisTemplate.opsForValue().set("auth:token:" + token, userDetails.getUsername(), 1, TimeUnit.HOURS);

        // Find user to return info
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在: " + request.getUsername()));
        
        return Result.success(new LoginResponse(token, user));
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
        boolean verified = verificationCodeService.verifyCode(request.getEmail(), request.getCode());
        if (!verified) {
            throw new IllegalArgumentException("验证码无效或已过期");
        }

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        userService.updatePassword(user.getId(), request.getNewPassword());
        return Result.success();
    }
    
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
    
    @Data
    public static class LoginResponse {
        private String token;
        private User user;
        
        public LoginResponse(String token, User user) {
            this.token = token;
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
