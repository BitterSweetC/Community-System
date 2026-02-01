package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.RecruitBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecruitBatchRepository extends JpaRepository<RecruitBatch, Long> {
    List<RecruitBatch> findByClubId(Long clubId);

    @Query("SELECT DISTINCT b.club FROM RecruitBatch b WHERE b.startTime <= :now AND b.endTime >= :now")
    List<Club> findClubsWithActiveRecruitment(@Param("now") LocalDateTime now);
}
