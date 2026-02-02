package com.cloud.community.notice.controller;

import com.cloud.community.core.common.PageResult;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.Notification;
import com.cloud.community.core.entity.User;
import com.cloud.community.notice.service.NotificationService;
import com.cloud.community.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return userService.findByUsername(((UserDetails) principal).getUsername()).orElseThrow();
        }
        throw new RuntimeException("Not authenticated");
    }

    @GetMapping
    public Result<PageResult<Notification>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = getCurrentUser();
        return Result.success(PageResult.of(notificationService.getUserNotifications(user.getId(), page, size)));
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        User user = getCurrentUser();
        return Result.success(notificationService.getUnreadCount(user.getId()));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        User user = getCurrentUser();
        notificationService.markAsRead(id, user.getId());
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        User user = getCurrentUser();
        notificationService.markAllAsRead(user.getId());
        return Result.success();
    }
}
