package com.cloud.community.core.repository;

import com.cloud.community.core.entity.ActivityAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityAttendanceRepository extends JpaRepository<ActivityAttendance, Long> {
    List<ActivityAttendance> findByActivityId(Long activityId);
    List<ActivityAttendance> findByUserId(Long userId);
}
