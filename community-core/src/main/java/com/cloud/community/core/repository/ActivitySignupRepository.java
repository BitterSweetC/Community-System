package com.cloud.community.core.repository;

import com.cloud.community.core.entity.ActivitySignup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivitySignupRepository extends JpaRepository<ActivitySignup, Long> {
    Optional<ActivitySignup> findByActivityIdAndUserId(Long activityId, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ActivitySignup s WHERE s.activity.id = :activityId AND s.user.id = :userId")
    Optional<ActivitySignup> findByActivityIdAndUserIdForUpdate(@Param("activityId") Long activityId,
                                                                 @Param("userId") Long userId);
    List<ActivitySignup> findByActivityId(Long activityId);
    List<ActivitySignup> findByUserId(Long userId);
}
