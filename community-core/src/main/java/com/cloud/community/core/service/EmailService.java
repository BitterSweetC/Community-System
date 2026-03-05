package com.cloud.community.core.service;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
}
