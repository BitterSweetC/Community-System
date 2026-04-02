package com.cloud.community.activity.service;

import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.entity.ActivityAttendance;
import com.cloud.community.core.entity.ActivitySignup;
import com.cloud.community.core.model.vo.ActivityRewardSettlementVO;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import com.cloud.community.core.model.dto.ActivityUpdateDTO;

import java.util.List;

public interface ActivityService {
    Activity createActivity(Activity activity, Long resourceApplicationId);

    List<Activity> getActivitiesByClub(Long clubId);

    Page<Activity> getActivitiesByClub(Long clubId, int page, int size);

    List<Activity> getAllActivities();

    Page<Activity> getAllActivities(int page, int size);

    Page<Activity> getActivities(Long clubId, String keyword, String clubName,
            LocalDateTime startTimeFrom,
            LocalDateTime startTimeTo,
            int page, int size);

    Activity getActivityById(Long id);

    void signup(Long activityId, Long userId);

    void signIn(Long activityId, Long userId, String code);

    List<ActivitySignup> getSignups(Long activityId);

    List<ActivityAttendance> getAttendances(Long activityId);

    List<ActivitySignup> getUserSignups(Long userId);

    void deleteActivity(Long id);

    Activity updateActivity(Long id, ActivityUpdateDTO dto);

    ActivityRewardSettlementVO settleRewards(Long activityId, Long operatorId);
}
