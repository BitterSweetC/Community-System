package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByClubId(Long clubId);
    org.springframework.data.domain.Page<Activity> findByClubId(Long clubId, org.springframework.data.domain.Pageable pageable);

    boolean existsByClubIdAndStatusIn(Long clubId, java.util.Collection<String> statuses);
}
