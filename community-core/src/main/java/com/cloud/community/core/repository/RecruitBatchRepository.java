package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.RecruitBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecruitBatchRepository extends JpaRepository<RecruitBatch, Long> {
    List<RecruitBatch> findByClubId(Long clubId);

    @Query("SELECT DISTINCT b.club FROM RecruitBatch b WHERE b.startTime <= :now AND b.endTime >= :now")
    List<Club> findClubsWithActiveRecruitment(@Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM RecruitBatch b WHERE b.id = :id")
    Optional<RecruitBatch> findByIdForUpdate(@Param("id") Long id);
}
