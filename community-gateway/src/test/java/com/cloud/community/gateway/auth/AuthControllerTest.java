package com.cloud.community.gateway.auth;

import com.cloud.community.core.entity.User;
import com.cloud.community.core.service.VerificationCodeService;
import com.cloud.community.gateway.controller.AuthController;
import com.cloud.community.gateway.security.JwtUtils;
import com.cloud.community.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock UserService userService;
    @Mock UserDetailsService userDetailsService;
    @Mock VerificationCodeService verificationCodeService;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtils jwtUtils;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks AuthController authController;

    @Test
    void login_throwsWhenRateLimitExceeded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(11L);
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(10L);

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("user");
        request.setPassword("pass");
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThatThrownBy(() -> authController.login(request, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("登录失败次数过多");
    }

    @Test
    void register_throwsWhenPasswordTooShort() {
        User user = new User();
        user.setPassword("abc1");

        assertThatThrownBy(() -> authController.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("密码至少8位");
    }

    @Test
    void register_throwsWhenPasswordHasNoDigit() {
        User user = new User();
        user.setPassword("onlyletters");

        assertThatThrownBy(() -> authController.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("密码至少8位");
    }

    @Test
    void resetPassword_throwsWhenCodeInvalid() {
        AuthController.ResetPasswordRequest request = new AuthController.ResetPasswordRequest();
        request.setEmail("user@example.com");
        request.setCode("000000");
        request.setNewPassword("ValidPass1");

        when(verificationCodeService.verifyCode("user@example.com", "000000")).thenReturn(false);

        assertThatThrownBy(() -> authController.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("验证码无效");
    }
}
