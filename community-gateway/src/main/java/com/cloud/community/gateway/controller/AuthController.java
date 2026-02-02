package com.cloud.community.gateway.controller;

import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.User;
import com.cloud.community.gateway.security.JwtUtils;
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
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        return Result.success(userService.register(user));
    }

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
        User user = userService.findByUsername(request.getUsername()).orElseThrow();
        
        return Result.success(new LoginResponse(token, user));
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
}
