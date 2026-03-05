package com.cloud.community.club.service;

import java.util.List;

public interface ChatService {
    String chat(String message, String sessionId);
    List<Long> getRecommendations(Long userId);
}
