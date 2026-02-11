package com.cloud.community.core.repository;

import com.cloud.community.core.entity.RecruitApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecruitApplicationRepository extends JpaRepository<RecruitApplication, Long> {
    List<RecruitApplication> findByBatchId(Long batchId);
    Optional<RecruitApplication> findByBatchIdAndUserId(Long batchId, Long userId);
    List<RecruitApplication> findByUserId(Long userId);

    long countByBatchIdAndFinalReviewStatus(Long batchId, String status);
}
