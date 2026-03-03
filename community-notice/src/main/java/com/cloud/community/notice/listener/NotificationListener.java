package com.cloud.community.notice.listener;

import com.cloud.community.core.constant.RabbitConstants;
import com.cloud.community.core.model.dto.NotificationMessageDTO;
import com.cloud.community.notice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConstants.ACTIVITY_SIGNUP_QUEUE)
    public void handleActivitySignup(NotificationMessageDTO message) {
        log.info("Received activity signup notification: {}", message);
        try {
            notificationService.sendNotification(
                    message.getUserId(),
                    message.getTitle(),
                    message.getContent(),
                    message.getType()
            );
        } catch (Exception e) {
            log.error("Failed to process notification message", e);
        }
    }

    @RabbitListener(queues = RabbitConstants.COMMON_NOTIFICATION_QUEUE)
    public void handleCommonNotification(NotificationMessageDTO message) {
        log.info("Received common notification: {}", message);
        try {
            notificationService.sendNotification(
                    message.getUserId(),
                    message.getTitle(),
                    message.getContent(),
                    message.getType()
            );
        } catch (Exception e) {
            log.error("Failed to process common notification message", e);
        }
    }

    @RabbitListener(queues = RabbitConstants.CLUB_BROADCAST_QUEUE)
    public void handleClubBroadcast(NotificationMessageDTO message) {
        log.info("Received club broadcast notification: {}", message);
        try {
            if (message.getClubId() != null) {
                notificationService.notifyClubMembers(
                        message.getClubId(),
                        message.getTitle(),
                        message.getContent()
                );
            } else {
                // 系统公告，通知所有用户
                notificationService.notifyAllUsers(
                        message.getTitle(),
                        message.getContent(),
                        message.getType() != null ? message.getType() : "SYSTEM"
                );
            }
        } catch (Exception e) {
            log.error("Failed to process club broadcast message", e);
        }
    }
}
