package com.cloud.community.club.service;

import java.util.List;

public interface ChatService {
    String chat(String message, String sessionId);

    default List<Long> getRecommendations(Long userId) {
        return getRecommendations(userId, "hybrid");
    }

    List<Long> getRecommendations(Long userId, String mode);
}
