package com.cloud.community.activity.service;

import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.entity.ActivityAttendance;
import com.cloud.community.core.entity.ActivitySignup;

import java.util.List;

public interface ActivityService {
    Activity createActivity(Activity activity);
    List<Activity> getActivitiesByClub(Long clubId);
    org.springframework.data.domain.Page<Activity> getActivitiesByClub(Long clubId, int page, int size);
    List<Activity> getAllActivities();
    org.springframework.data.domain.Page<Activity> getAllActivities(int page, int size);
    Activity getActivityById(Long id);
    
    void signup(Long activityId, Long userId);
    void signIn(Long activityId, Long userId, String code);
    List<ActivitySignup> getSignups(Long activityId);
    List<ActivityAttendance> getAttendances(Long activityId);
    List<ActivitySignup> getUserSignups(Long userId);
    void deleteActivity(Long id);
}
