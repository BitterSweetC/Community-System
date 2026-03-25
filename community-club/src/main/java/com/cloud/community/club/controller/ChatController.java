package com.cloud.community.club.controller;

import com.cloud.community.club.service.ChatService;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.Member;
import com.cloud.community.core.entity.Role;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/club/chat")
@RequiredArgsConstructor
public class ChatController {
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long RATE_WINDOW_MILLIS = 60_000L;
    private static final long STALE_WINDOW_MILLIS = RATE_WINDOW_MILLIS * 2;

    private final ConcurrentHashMap<String, RateWindow> rateWindows = new ConcurrentHashMap<>();
    private final ChatService chatService;
    private final UserService userService;
    private final MemberRepository memberRepository;

    @PostMapping
    public Result<String> chat(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String message = request.get("message");
        String sessionId = resolveSessionId(request.get("sessionId"));
        String limiterKey = resolveLimiterKey(sessionId, httpRequest);
        if (isRateLimited(limiterKey)) {
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        String response = chatService.chat(message, sessionId, buildUserContext());
        return Result.success(response);
    }

    private Map<String, Object> buildUserContext() {
        Map<String, Object> context = new HashMap<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        context.put("authenticated", authenticated);
        if (!authenticated) {
            return context;
        }

        return userService.findByUsername(authentication.getName())
                .map(this::toUserContext)
                .orElse(context);
    }

    private Map<String, Object> toUserContext(User user) {
        Map<String, Object> context = new HashMap<>();
        context.put("authenticated", true);
        context.put("user_id", user.getId());
        context.put("username", user.getUsername());
        context.put(
                "role_codes",
                user.getRoles().stream()
                        .map(Role::getCode)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .collect(Collectors.toList())
        );

        List<Member> managedMembers = memberRepository.findManagedClubs(
                user.getId(),
                "ACTIVE",
                List.of("PRESIDENT", "MANAGER")
        );
        context.put(
                "managed_club_ids",
                managedMembers.stream()
                        .map(Member::getClub)
                        .filter(Objects::nonNull)
                        .filter(club -> Club.STATUS_ACTIVE.equals(club.getStatus()))
                        .map(Club::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList())
        );
        context.put(
                "managed_club_names",
                managedMembers.stream()
                        .map(Member::getClub)
                        .filter(Objects::nonNull)
                        .filter(club -> Club.STATUS_ACTIVE.equals(club.getStatus()))
                        .map(Club::getName)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .collect(Collectors.toList())
        );
        return context;
    }

    private String resolveSessionId(String requestSessionId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user-" + authentication.getName();
        }
        if (requestSessionId == null) {
            return null;
        }
        String trimmed = requestSessionId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveLimiterKey(String sessionId, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }
        if (StringUtils.hasText(sessionId)) {
            return "session:" + sessionId;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String first = forwarded.split(",")[0].trim();
            if (StringUtils.hasText(first)) {
                return "ip:" + first;
            }
        }
        return "ip:" + request.getRemoteAddr();
    }

    private boolean isRateLimited(String key) {
        long now = System.currentTimeMillis();
        cleanupStaleEntries(now);

        RateWindow window = rateWindows.computeIfAbsent(key, ignored -> new RateWindow(now));
        synchronized (window) {
            if (now - window.windowStartMillis >= RATE_WINDOW_MILLIS) {
                window.windowStartMillis = now;
                window.requestCount = 0;
            }
            if (window.requestCount >= MAX_REQUESTS_PER_MINUTE) {
                return true;
            }
            window.requestCount++;
            return false;
        }
    }

    private void cleanupStaleEntries(long now) {
        if (rateWindows.size() <= 2000) {
            return;
        }
        Iterator<Map.Entry<String, RateWindow>> iterator = rateWindows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, RateWindow> entry = iterator.next();
            if (now - entry.getValue().windowStartMillis > STALE_WINDOW_MILLIS) {
                iterator.remove();
            }
        }
    }

    private static class RateWindow {
        private long windowStartMillis;
        private int requestCount;

        private RateWindow(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
            this.requestCount = 0;
        }
    }
}
