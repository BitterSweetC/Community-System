package com.cloud.community.club.controller;

import com.cloud.community.club.service.ChatService;
import com.cloud.community.core.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/club/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public Result<String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String response = chatService.chat(message);
        return Result.success(response);
    }
}
