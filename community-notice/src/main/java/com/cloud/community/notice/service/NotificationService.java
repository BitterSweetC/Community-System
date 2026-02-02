package com.cloud.community.notice.service;

import com.cloud.community.core.entity.Notification;
import org.springframework.data.domain.Page;

public interface NotificationService {
    void sendNotification(Long userId, String title, String content, String type);
    Page<Notification> getUserNotifications(Long userId, int page, int size);
    void markAsRead(Long id, Long userId);
    void markAllAsRead(Long userId);
    long getUnreadCount(Long userId);
    void notifyClubMembers(Long clubId, String title, String content);
}
