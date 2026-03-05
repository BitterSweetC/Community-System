package com.cloud.community.core.repository;

import com.cloud.community.core.entity.RecruitApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecruitApplicationRepository extends JpaRepository<RecruitApplication, Long> {
    List<RecruitApplication> findByBatchId(Long batchId);
    Optional<RecruitApplication> findByBatchIdAndUserId(Long batchId, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM RecruitApplication a WHERE a.id = :id")
    Optional<RecruitApplication> findByIdForUpdate(@Param("id") Long id);
    List<RecruitApplication> findByUserId(Long userId);

    long countByBatchIdAndFinalReviewStatus(Long batchId, String status);
}
