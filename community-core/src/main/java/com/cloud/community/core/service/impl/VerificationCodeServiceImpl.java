package com.cloud.community.core.service.impl;

import com.cloud.community.core.service.EmailService;
import com.cloud.community.core.service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EmailService emailService;

    private static final String CODE_PREFIX = "auth:code:";
    private static final long EXPIRATION_MINUTES = 5;

    @Override
    public void sendVerificationCode(String email) {
        // Generate 6 digit code
        String code = String.valueOf(new Random().nextInt(900000) + 100000);
        
        // Save to Redis
        String key = CODE_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(EXPIRATION_MINUTES));

        // Send Email
        String subject = "【社团管理系统】密码重置验证码";
        String text = "您的验证码是：" + code + "。有效期为" + EXPIRATION_MINUTES + "分钟。如果不是您本人的操作，请忽略此邮件。";
        
        emailService.sendSimpleMessage(email, subject, text);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String key = CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode != null && storedCode.equals(code)) {
            redisTemplate.delete(key); // Use once
            return true;
        }
        return false;
    }
}
