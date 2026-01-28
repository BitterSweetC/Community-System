package com.cloud.community.core.repository;

import com.cloud.community.core.entity.RecruitBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecruitBatchRepository extends JpaRepository<RecruitBatch, Long> {
    List<RecruitBatch> findByClubId(Long clubId);
}
