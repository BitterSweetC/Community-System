package com.cloud.community.activity.service.impl;

import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.entity.ActivityAttendance;
import com.cloud.community.core.entity.ResourceApplication;
import com.cloud.community.core.entity.ActivitySignup;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.exception.BusinessException;
import com.cloud.community.core.model.dto.ActivityUpdateDTO;
import com.cloud.community.core.model.vo.ActivityRewardSettlementVO;
import com.cloud.community.core.repository.ActivityAttendanceRepository;
import com.cloud.community.core.repository.ActivityRepository;
import com.cloud.community.core.repository.ActivitySignupRepository;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.core.repository.ResourceApplicationRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.core.service.MemberArchiveService;
import com.cloud.community.activity.service.ActivityService;
import com.cloud.community.notice.service.NotificationService;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.core.constant.RabbitConstants;
import com.cloud.community.core.model.dto.NotificationMessageDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final Sort DEFAULT_ACTIVITY_SORT = Sort.by(Sort.Direction.DESC, "startTime");
    private static final DateTimeFormatter ACTIVITY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ActivityRepository activityRepository;
    private final ActivitySignupRepository signupRepository;
    private final ActivityAttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final ResourceApplicationRepository resourceApplicationRepository;
    private final PermissionService permissionService;
    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final MemberArchiveService memberArchiveService;

    @Override
    @Transactional
    public Activity createActivity(Activity activity, Long resourceApplicationId) {
        permissionService.checkClubActive(activity.getClub().getId());
        if (activity.getNeedAttendance() == null) {
            activity.setNeedAttendance(false);
        }
        if (activity.getRewardPoints() == null) {
            activity.setRewardPoints(0);
        }
        if (activity.getSettlementStatus() == null) {
            activity.setSettlementStatus("PENDING");
        }
        activity.setStatus("PUBLISHED");

        ResourceApplication selectedVenue = null;
        if (isOnlineActivityType(activity.getType())) {
            activity.setLocation(null);
        } else {
            selectedVenue = lockAndValidateVenueApplication(activity.getClub().getId(), resourceApplicationId, null);
            applyVenueBinding(activity, selectedVenue);
        }

        Activity savedActivity = activityRepository.save(activity);
        bindVenueApplication(savedActivity.getId(), selectedVenue);
        return savedActivity;
    }

    @Override
    public List<Activity> getActivitiesByClub(Long clubId) {
        return activityRepository.findByClubId(clubId).stream()
                .sorted(Comparator.comparing(Activity::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .toList();
    }

    @Override
    public Page<Activity> getActivitiesByClub(Long clubId, int page, int size) {
        return activityRepository.findByClubId(clubId, PageRequest.of(page, size, DEFAULT_ACTIVITY_SORT));
    }

    @Override
    public List<Activity> getAllActivities() {
        return activityRepository.findAll(DEFAULT_ACTIVITY_SORT);
    }

    @Override
    public Page<Activity> getAllActivities(int page, int size) {
        return activityRepository.findAll(PageRequest.of(page, size, DEFAULT_ACTIVITY_SORT));
    }

    @Override
    public Page<Activity> getActivities(Long clubId, String keyword, String clubName,
            LocalDateTime startTimeFrom,
            LocalDateTime startTimeTo,
            int page, int size) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedClubName = StringUtils.hasText(clubName) ? clubName.trim() : null;
        return activityRepository.searchActivities(
                clubId,
                normalizedKeyword,
                normalizedClubName,
                startTimeFrom,
                startTimeTo,
                PageRequest.of(page, size, DEFAULT_ACTIVITY_SORT));
    }

    @Override
    public Activity getActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("未找到活动"));
    }

    @Override
    @Transactional
    public void signup(Long activityId, Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("未找到用户"));

        if (signupRepository.findByActivityIdAndUserId(activityId, userId).isPresent()) {
            throw new BusinessException(40901, "您已报名该活动，请勿重复报名");
        }

        Activity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new RuntimeException("未找到活动"));

        // Check if user is a member of the club (only for club activities)
        if (activity.getClub() != null) {
            boolean isMember = memberRepository.findByClubIdAndUserId(activity.getClub().getId(), userId).isPresent();
            if (!isMember) {
                throw new IllegalArgumentException("请先加入" + activity.getClub().getName() + "社团");
            }
        }

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
                    message);
        } catch (Exception e) {
            // Log error but don't fail the transaction
            System.err.println("Failed to send notification message: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void signIn(Long activityId, Long userId, String code) {
        ActivitySignup signup = signupRepository.findByActivityIdAndUserIdForUpdate(activityId, userId)
                .orElseThrow(() -> new RuntimeException("未找到报名记录"));

        Activity activity = signup.getActivity();

        // Check activity time window
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new RuntimeException("活动尚未开始，无法签到");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new RuntimeException("活动已结束，无法签到");
        }

        // Check check-in code if set
        if (activity.getCheckinCode() != null && !activity.getCheckinCode().isEmpty()) {
            if (code == null || !code.trim().equals(activity.getCheckinCode())) {
                throw new RuntimeException("签到码错误，无法签到");
            }
        }

        if ("SIGNED_IN".equals(signup.getStatus())) {
            throw new BusinessException(40902, "您已签到，请勿重复签到");
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

        // Broadcast real-time check-in count to subscribers
        long checkinCount = attendanceRepository.countByActivityId(activityId);
        java.util.Map<String, Object> payload = java.util.Map.of(
                "activityId", activityId, "checkinCount", checkinCount);
        messagingTemplate.convertAndSend("/topic/activity/" + activityId + "/checkin", payload);
        messagingTemplate.convertAndSend("/topic/checkin/feed", payload);

        // Send notification
        notificationService.sendNotification(
                userId,
                "活动签到成功",
                "您已成功签到活动：" + signup.getActivity().getTitle(),
                "ACTIVITY");
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
    public Activity updateActivity(Long id, ActivityUpdateDTO dto) {
        Activity activity = activityRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("未找到活动"));
        if (!LocalDateTime.now().isBefore(activity.getStartTime())) {
            throw new BusinessException(40903, "活动已开始，无法修改");
        }

        permissionService.checkClubActive(activity.getClub().getId());
        String previousTitle = activity.getTitle();
        ResourceApplication currentVenue = getCurrentVenueBindingForUpdate(activity.getId());
        boolean settlementDirty = false;
        if (dto.getTitle() != null)
            activity.setTitle(dto.getTitle());
        if (dto.getDescription() != null)
            activity.setDescription(dto.getDescription());
        if (dto.getCoverUrl() != null)
            activity.setCoverUrl(dto.getCoverUrl());
        if (dto.getType() != null)
            activity.setType(dto.getType());
        if (dto.getStartTime() != null)
            activity.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null)
            activity.setEndTime(dto.getEndTime());
        if (dto.getSignupStartTime() != null)
            activity.setSignupStartTime(dto.getSignupStartTime());
        if (dto.getSignupEndTime() != null)
            activity.setSignupEndTime(dto.getSignupEndTime());
        if (dto.getMaxParticipants() != null)
            activity.setMaxParticipants(dto.getMaxParticipants());
        if (dto.getNeedAttendance() != null && !Objects.equals(dto.getNeedAttendance(), activity.getNeedAttendance())) {
            activity.setNeedAttendance(dto.getNeedAttendance());
            settlementDirty = true;
        }
        if (dto.getCheckinCode() != null)
            activity.setCheckinCode(dto.getCheckinCode());
        if (dto.getRewardPoints() != null && !Objects.equals(dto.getRewardPoints(), activity.getRewardPoints())) {
            activity.setRewardPoints(dto.getRewardPoints());
            settlementDirty = true;
        }
        if (settlementDirty) {
            activity.setSettlementStatus("PENDING");
            activity.setSettledAt(null);
        }

        ResourceApplication selectedVenue = null;
        if (isOnlineActivityType(activity.getType())) {
            activity.setLocation(null);
        } else {
            Long effectiveResourceApplicationId = dto.getResourceApplicationId();
            if (effectiveResourceApplicationId == null && currentVenue != null) {
                effectiveResourceApplicationId = currentVenue.getId();
            }
            selectedVenue = lockAndValidateVenueApplication(activity.getClub().getId(),
                    effectiveResourceApplicationId, activity.getId());
            applyVenueBinding(activity, selectedVenue);
        }

        Activity savedActivity = activityRepository.save(activity);
        syncVenueBinding(savedActivity.getId(), currentVenue, selectedVenue);
        notifySignedUsersAboutUpdate(savedActivity, previousTitle);
        return savedActivity;
    }

    @Override
    @Transactional
    public ActivityRewardSettlementVO settleRewards(Long activityId, Long operatorId) {
        return memberArchiveService.settleActivityRewards(activityId, operatorId);
    }

    @Override
    @Transactional
    public void deleteActivity(Long id) {
        Activity activity = activityRepository.findByIdWithClub(id)
                .orElseThrow(() -> new RuntimeException("未找到活动"));
        permissionService.checkClubActive(activity.getClub().getId());

        // Notify signed up users
        List<ActivitySignup> signups = signupRepository.findByActivityId(id);
        for (ActivitySignup signup : signups) {
            try {
                NotificationMessageDTO message = new NotificationMessageDTO();
                message.setUserId(signup.getUser().getId());
                message.setTitle("活动取消通知");
                message.setContent("您报名的活动 '" + activity.getTitle() + "' 已被取消。");
                message.setType("SYSTEM");
                rabbitTemplate.convertAndSend(RabbitConstants.NOTIFICATION_EXCHANGE,
                        RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY, message);
            } catch (Exception e) {
                log.error("Failed to send cancellation notification to user {}", signup.getUser().getId(), e);
            }
        }

        // 先删除关联记录
        List<ActivityAttendance> attendances = attendanceRepository.findByActivityId(id);
        attendanceRepository.deleteAll(attendances);
        signupRepository.deleteAll(signups);

        // 再删除活动
        activityRepository.deleteById(id);
    }

    private boolean isOnlineActivityType(String type) {
        return "Online".equalsIgnoreCase(type);
    }

    private ResourceApplication lockAndValidateVenueApplication(Long clubId, Long resourceApplicationId,
            Long currentActivityId) {
        if (resourceApplicationId == null) {
            throw new BusinessException(40043, "线下活动必须选择已批准的场地资源");
        }

        ResourceApplication application = resourceApplicationRepository.findByIdForUpdate(resourceApplicationId)
                .orElseThrow(() -> new RuntimeException("未找到资源申请"));

        if (!Objects.equals(application.getClubId(), clubId)) {
            throw new BusinessException(40904, "仅可绑定当前社团已申请到的场地资源");
        }
        if (!"APPROVED".equals(application.getStatus())
                || application.getResource() == null
                || !"VENUE".equals(application.getResource().getType())) {
            throw new BusinessException(40905, "仅可绑定已批准的场地资源");
        }
        if (application.getEndTime() == null || !application.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(40907, "所选场地资源已过期，无法绑定");
        }
        if (application.getActivityId() != null && !Objects.equals(application.getActivityId(), currentActivityId)) {
            throw new BusinessException(40906, "所选场地资源已绑定其他活动");
        }
        return application;
    }

    private ResourceApplication getCurrentVenueBindingForUpdate(Long activityId) {
        List<ResourceApplication> bindings = resourceApplicationRepository.findVenueByActivityIdForUpdate(activityId);
        if (bindings.isEmpty()) {
            return null;
        }
        if (bindings.size() > 1) {
            log.warn("Detected multiple venue bindings for activity {}, using the earliest application {}", activityId,
                    bindings.get(0).getId());
        }
        return bindings.get(0);
    }

    private void applyVenueBinding(Activity activity, ResourceApplication application) {
        activity.setStartTime(application.getStartTime());
        activity.setEndTime(application.getEndTime());
        activity.setLocation(application.getResource() != null ? application.getResource().getLocation() : null);
    }

    private void bindVenueApplication(Long activityId, ResourceApplication venueApplication) {
        if (venueApplication == null) {
            return;
        }
        venueApplication.setActivityId(activityId);
        resourceApplicationRepository.save(venueApplication);
    }

    private void syncVenueBinding(Long activityId, ResourceApplication currentVenue,
            ResourceApplication selectedVenue) {
        if (currentVenue != null
                && (selectedVenue == null || !Objects.equals(currentVenue.getId(), selectedVenue.getId()))) {
            currentVenue.setActivityId(null);
            resourceApplicationRepository.save(currentVenue);
        }

        if (selectedVenue != null && !Objects.equals(selectedVenue.getActivityId(), activityId)) {
            selectedVenue.setActivityId(activityId);
            resourceApplicationRepository.save(selectedVenue);
        }
    }

    private void notifySignedUsersAboutUpdate(Activity activity, String previousTitle) {
        List<ActivitySignup> signups = signupRepository.findByActivityId(activity.getId());
        if (signups.isEmpty()) {
            return;
        }

        String messageContent = buildActivityUpdateMessage(activity, previousTitle);
        for (ActivitySignup signup : signups) {
            try {
                NotificationMessageDTO message = new NotificationMessageDTO();
                message.setUserId(signup.getUser().getId());
                message.setTitle("活动信息已变更");
                message.setContent(messageContent);
                message.setType("ACTIVITY");
                rabbitTemplate.convertAndSend(
                        RabbitConstants.NOTIFICATION_EXCHANGE,
                        RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY,
                        message);
            } catch (Exception e) {
                log.error("Failed to send activity update notification to user {}", signup.getUser().getId(), e);
            }
        }
    }

    private String buildActivityUpdateMessage(Activity activity, String previousTitle) {
        StringBuilder builder = new StringBuilder();
        builder.append("您报名的活动“").append(activity.getTitle()).append("”信息已更新。");
        if (previousTitle != null && !previousTitle.equals(activity.getTitle())) {
            builder.append("原活动名称为“").append(previousTitle).append("”。");
        }
        builder.append("最新时间：")
                .append(formatActivityTime(activity.getStartTime()))
                .append(" - ")
                .append(formatActivityTime(activity.getEndTime()))
                .append("。");
        builder.append("最新地点：")
                .append(StringUtils.hasText(activity.getLocation()) ? activity.getLocation() : "线上活动")
                .append("。");
        return builder.toString();
    }

    private String formatActivityTime(LocalDateTime time) {
        return time == null ? "待定" : time.format(ACTIVITY_TIME_FORMATTER);
    }
}
