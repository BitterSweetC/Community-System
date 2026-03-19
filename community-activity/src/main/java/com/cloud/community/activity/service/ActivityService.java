package com.cloud.community.activity.service;

import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.entity.ActivityAttendance;
import com.cloud.community.core.entity.ActivitySignup;
import com.cloud.community.core.model.vo.ActivityRewardSettlementVO;

import java.util.List;

public interface ActivityService {
    Activity createActivity(Activity activity);
    List<Activity> getActivitiesByClub(Long clubId);
    org.springframework.data.domain.Page<Activity> getActivitiesByClub(Long clubId, int page, int size);
    List<Activity> getAllActivities();
    org.springframework.data.domain.Page<Activity> getAllActivities(int page, int size);
    org.springframework.data.domain.Page<Activity> getActivities(Long clubId, String keyword, String clubName,
                                                                 java.time.LocalDateTime startTimeFrom,
                                                                 java.time.LocalDateTime startTimeTo,
                                                                 int page, int size);
    Activity getActivityById(Long id);
    
    void signup(Long activityId, Long userId);
    void signIn(Long activityId, Long userId, String code);
    List<ActivitySignup> getSignups(Long activityId);
    List<ActivityAttendance> getAttendances(Long activityId);
    List<ActivitySignup> getUserSignups(Long userId);
    void deleteActivity(Long id);
    Activity updateActivity(Long id, com.cloud.community.core.model.dto.ActivityUpdateDTO dto);
    ActivityRewardSettlementVO settleRewards(Long activityId, Long operatorId);
}
