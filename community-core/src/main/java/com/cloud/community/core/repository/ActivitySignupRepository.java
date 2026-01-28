package com.cloud.community.core.repository;

import com.cloud.community.core.entity.ActivitySignup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivitySignupRepository extends JpaRepository<ActivitySignup, Long> {
    Optional<ActivitySignup> findByActivityIdAndUserId(Long activityId, Long userId);
    List<ActivitySignup> findByActivityId(Long activityId);
    List<ActivitySignup> findByUserId(Long userId);
}
