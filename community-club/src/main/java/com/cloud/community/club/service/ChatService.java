package com.cloud.community.club.service;

import java.util.List;
import java.util.Map;

public interface ChatService {
    default String chat(String message, String sessionId) {
        return chat(message, sessionId, java.util.Collections.emptyMap());
    }

    String chat(String message, String sessionId, Map<String, Object> userContext);

    default List<Long> getRecommendations(Long userId) {
        return getRecommendations(userId, "hybrid");
    }

    List<Long> getRecommendations(Long userId, String mode);
}
