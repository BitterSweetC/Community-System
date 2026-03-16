package com.cloud.community.gateway.auth;

import com.cloud.community.core.entity.User;
import com.cloud.community.core.metrics.BusinessMetricsService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock UserService userService;
    @Mock UserDetailsService userDetailsService;
    @Mock VerificationCodeService verificationCodeService;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtils jwtUtils;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock BusinessMetricsService metricsService;
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
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_throwsWhenPasswordTooShort() {
        User user = new User();
        user.setPassword("abc1");

        assertThatThrownBy(() -> authController.register(user))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_throwsWhenPasswordHasNoDigit() {
        User user = new User();
        user.setPassword("onlyletters");

        assertThatThrownBy(() -> authController.register(user))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetPassword_throwsWhenCodeInvalid() {
        AuthController.ResetPasswordRequest request = new AuthController.ResetPasswordRequest();
        request.setEmail("user@example.com");
        request.setCode("000000");
        request.setNewPassword("ValidPass1");

        when(verificationCodeService.verifyCode("user@example.com", "000000")).thenReturn(false);

        assertThatThrownBy(() -> authController.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refresh_throwsWhenUserDisabled() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("auth:refresh:disabled-user")).thenReturn("refresh-token");
        when(jwtUtils.extractUsername("refresh-token")).thenReturn("disabled-user");
        when(jwtUtils.isRefreshTokenValid("refresh-token", "disabled-user")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("disabled-user")).thenReturn(
                new org.springframework.security.core.userdetails.User(
                        "disabled-user", "encoded", false, true, true, true, java.util.List.of()
                )
        );

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token"));
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();

        assertThatThrownBy(() -> authController.refresh(request, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disabled");

        verify(redisTemplate).delete("auth:refresh:disabled-user");
    }
}