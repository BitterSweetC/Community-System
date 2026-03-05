package com.cloud.community.core.service;

public interface VerificationCodeService {
    void sendVerificationCode(String email);
    boolean verifyCode(String email, String code);
}
