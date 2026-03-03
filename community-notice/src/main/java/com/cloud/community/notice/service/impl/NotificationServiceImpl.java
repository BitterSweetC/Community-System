package com.cloud.community.notice.service.impl;

import com.cloud.community.core.entity.Member;
import com.cloud.community.core.entity.Notification;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.core.repository.NotificationRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.notice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void sendNotification(Long userId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    @Override
    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public void markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void notifyClubMembers(Long clubId, String title, String content) {
        List<Member> members = memberRepository.findByClubId(clubId);
        List<Notification> notifications = members.stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .map(member -> {
                    Notification n = new Notification();
                    n.setUserId(member.getUser().getId());
                    n.setTitle(title);
                    n.setContent(content);
                    n.setType("CLUB");
                    n.setIsRead(false);
                    return n;
                })
                .toList();
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void notifyAllUsers(String title, String content, String type) {
        List<User> users = userRepository.findAll();
        List<Notification> notifications = users.stream()
                .map(user -> {
                    Notification n = new Notification();
                    n.setUserId(user.getId());
                    n.setTitle(title);
                    n.setContent(content);
                    n.setType(type);
                    n.setIsRead(false);
                    return n;
                })
                .toList();
        notificationRepository.saveAll(notifications);
    }
}
