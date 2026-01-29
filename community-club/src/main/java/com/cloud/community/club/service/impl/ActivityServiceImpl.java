package com.cloud.community.club.service.impl;

import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.entity.ActivitySignup;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.ActivityRepository;
import com.cloud.community.core.repository.ActivitySignupRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.club.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivitySignupRepository signupRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Activity createActivity(Activity activity) {
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ActivitySignup signup = new ActivitySignup();
        signup.setActivity(activity);
        signup.setUser(user);
        signup.setStatus("SIGNED");
        
        signupRepository.save(signup);
    }

    @Override
    @Transactional
    public void signIn(Long activityId, Long userId) {
        ActivitySignup signup = signupRepository.findByActivityIdAndUserId(activityId, userId)
                .orElseThrow(() -> new RuntimeException("Signup not found"));
        
        if ("SIGNED_IN".equals(signup.getStatus())) {
             throw new RuntimeException("Already signed in");
        }
        
        signup.setStatus("SIGNED_IN");
        signupRepository.save(signup);
    }

    @Override
    public List<ActivitySignup> getSignups(Long activityId) {
        return signupRepository.findByActivityId(activityId);
    }

    @Override
    @Transactional
    public void deleteActivity(Long id) {
        activityRepository.deleteById(id);
    }
}
