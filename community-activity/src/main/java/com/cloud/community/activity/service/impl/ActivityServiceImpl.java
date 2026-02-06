package com.cloud.community.activity.service.impl;

import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.entity.ActivityAttendance;
import com.cloud.community.core.entity.ActivitySignup;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.ActivityAttendanceRepository;
import com.cloud.community.core.repository.ActivityRepository;
import com.cloud.community.core.repository.ActivitySignupRepository;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.activity.service.ActivityService;
import com.cloud.community.notice.service.NotificationService;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.core.constant.RabbitConstants;
import com.cloud.community.core.model.dto.NotificationMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivitySignupRepository signupRepository;
    private final ActivityAttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PermissionService permissionService;
    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public Activity createActivity(Activity activity) {
        permissionService.checkClubActive(activity.getClub().getId());
        return activityRepository.save(activity);
    }

    @Override
    public List<Activity> getActivitiesByClub(Long clubId) {
        return activityRepository.findByClubId(clubId);
    }

    @Override
    public org.springframework.data.domain.Page<Activity> getActivitiesByClub(Long clubId, int page, int size) {
        return activityRepository.findByClubId(clubId, org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Override
    public List<Activity> getAllActivities() {
        return activityRepository.findAll();
    }

    @Override
    public org.springframework.data.domain.Page<Activity> getAllActivities(int page, int size) {
        return activityRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Override
    public Activity getActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
    }

    @Override
    @Transactional
    public void signup(Long activityId, Long userId) {
        if (signupRepository.findByActivityIdAndUserId(activityId, userId).isPresent()) {
            throw new RuntimeException("Already signed up");
        }
        
        Activity activity = getActivityById(activityId);

        // Check if user is a member of the club
        boolean isMember = memberRepository.findByClubIdAndUserId(activity.getClub().getId(), userId).isPresent();
        if (!isMember) {
            throw new IllegalArgumentException("请先加入" + activity.getClub().getName() + "社团");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ActivitySignup signup = new ActivitySignup();
        signup.setActivity(activity);
        signup.setUser(user);
        signup.setStatus("SIGNED");
        
        signupRepository.save(signup);

        // Send notification asynchronously via RabbitMQ
        try {
            NotificationMessageDTO message = new NotificationMessageDTO();
            message.setUserId(userId);
            message.setTitle("活动报名成功");
            message.setContent("您已成功报名活动：" + activity.getTitle());
            message.setType("ACTIVITY");

            rabbitTemplate.convertAndSend(
                    RabbitConstants.NOTIFICATION_EXCHANGE,
                    RabbitConstants.ACTIVITY_SIGNUP_ROUTING_KEY,
                    message
            );
        } catch (Exception e) {
            // Log error but don't fail the transaction
            // Ideally we should have a fallback or retry mechanism
            System.err.println("Failed to send notification message: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void signIn(Long activityId, Long userId, String code) {
        ActivitySignup signup = signupRepository.findByActivityIdAndUserId(activityId, userId)
                .orElseThrow(() -> new RuntimeException("Signup not found"));

        Activity activity = signup.getActivity();
        
        // Check check-in code if set
        if (activity.getCheckinCode() != null && !activity.getCheckinCode().isEmpty()) {
            if (code == null || !code.trim().equals(activity.getCheckinCode())) {
                throw new RuntimeException("Invalid check-in code");
            }
        }
        
        if ("SIGNED_IN".equals(signup.getStatus())) {
             throw new RuntimeException("Already signed in");
        }
        
        signup.setStatus("SIGNED_IN");
        signupRepository.save(signup);

        // Record attendance
        ActivityAttendance attendance = new ActivityAttendance();
        attendance.setActivity(signup.getActivity());
        attendance.setUser(signup.getUser());
        attendance.setSignTime(LocalDateTime.now());
        attendance.setSource("WEB"); // Default source
        attendanceRepository.save(attendance);

        // Send notification
        notificationService.sendNotification(
            userId, 
            "活动签到成功", 
            "您已成功签到活动：" + signup.getActivity().getTitle(), 
            "ACTIVITY"
        );
    }

    @Override
    public List<ActivitySignup> getSignups(Long activityId) {
        return signupRepository.findByActivityId(activityId);
    }

    @Override
    public List<ActivityAttendance> getAttendances(Long activityId) {
        return attendanceRepository.findByActivityId(activityId);
    }

    @Override
    public List<ActivitySignup> getUserSignups(Long userId) {
        return signupRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteActivity(Long id) {
        Activity activity = getActivityById(id);
        permissionService.checkClubActive(activity.getClub().getId());
        
        // Notify signed up users
        List<ActivitySignup> signups = signupRepository.findByActivityId(id);
        for (ActivitySignup signup : signups) {
            try {
                NotificationMessageDTO message = new NotificationMessageDTO();
                message.setUserId(signup.getUser().getId());
                message.setTitle("Activity Cancelled");
                message.setContent("The activity '" + activity.getTitle() + "' you signed up for has been cancelled.");
                message.setType("SYSTEM");
                rabbitTemplate.convertAndSend(RabbitConstants.NOTIFICATION_EXCHANGE, RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        activityRepository.deleteById(id);
    }
}
