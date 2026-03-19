package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Activity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    @Query("SELECT a FROM Activity a JOIN FETCH a.club WHERE a.id = :id")
    Optional<Activity> findByIdWithClub(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Activity a WHERE a.id = :id")
    Optional<Activity> findByIdForUpdate(@Param("id") Long id);

    List<Activity> findByClubId(Long clubId);
    org.springframework.data.domain.Page<Activity> findByClubId(Long clubId, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT a FROM Activity a
            JOIN a.club c
            WHERE (:clubId IS NULL OR c.id = :clubId)
              AND (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:clubName IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :clubName, '%')))
              AND (:startTimeFrom IS NULL OR a.startTime >= :startTimeFrom)
              AND (:startTimeTo IS NULL OR a.startTime <= :startTimeTo)
            """)
    Page<Activity> searchActivities(@Param("clubId") Long clubId,
                                    @Param("keyword") String keyword,
                                    @Param("clubName") String clubName,
                                    @Param("startTimeFrom") LocalDateTime startTimeFrom,
                                    @Param("startTimeTo") LocalDateTime startTimeTo,
                                    Pageable pageable);

    boolean existsByClubIdAndStatusIn(Long clubId, java.util.Collection<String> statuses);

    long countByClubId(Long clubId);

    long countByClubIdAndStartTimeAfter(Long clubId, LocalDateTime time);

    // 查询需要推进到进行中的活动（已发布且开始时间已到）
    @Modifying
    @Query("UPDATE Activity a SET a.status = 'IN_PROGRESS' WHERE a.status = 'PUBLISHED' AND a.startTime <= :now")
    int markInProgress(@Param("now") LocalDateTime now);

    // 查询需要推进到已结束的活动（进行中且结束时间已过）
    @Modifying
    @Query("UPDATE Activity a SET a.status = 'ENDED' WHERE a.status = 'IN_PROGRESS' AND a.endTime <= :now")
    int markEnded(@Param("now") LocalDateTime now);

    List<Activity> findByStatusAndSettlementStatus(String status, String settlementStatus);
}
