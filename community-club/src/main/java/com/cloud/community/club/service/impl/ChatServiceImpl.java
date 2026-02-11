package com.cloud.community.club.service.impl;

import com.cloud.community.club.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    @Value("${rag.service.url:http://localhost:8000}")
    private String ragServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String chat(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Please enter a message.";
        }

        // Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build body
        Map<String, String> body = new HashMap<>();
        body.put("query", message);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            // Call RAG Agent
            String url = ragServiceUrl + "/chat";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("response")) {
                return (String) response.getBody().get("response");
            }
            return "No response from AI Agent.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling AI Agent: " + e.getMessage() + ". Please ensure the RAG service is running.";
        }
    }

    @Override
    public java.util.List<Long> getRecommendations(Long userId) {
        if (userId == null) return java.util.Collections.emptyList();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("top_k", 5);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            String url = ragServiceUrl + "/recommend";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("club_ids")) {
                java.util.List<?> rawIds = (java.util.List<?>) response.getBody().get("club_ids");
                return rawIds.stream()
                        .filter(item -> item instanceof Number)
                        .map(item -> ((Number) item).longValue())
                        .collect(java.util.stream.Collectors.toList());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return java.util.Collections.emptyList();
    }
}
