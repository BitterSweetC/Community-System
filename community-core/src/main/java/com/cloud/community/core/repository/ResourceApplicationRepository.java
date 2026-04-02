package com.cloud.community.core.repository;

import com.cloud.community.core.entity.ResourceApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ResourceApplicationRepository extends JpaRepository<ResourceApplication, Long> {
    List<ResourceApplication> findByClubId(Long clubId);
    Page<ResourceApplication> findByClubIdOrderByCreatedAtDesc(Long clubId, Pageable pageable);
    List<ResourceApplication> findByStatus(String status);
    Page<ResourceApplication> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<ResourceApplication> findByStatusOrderByCreatedAtDesc(String status);
    List<ResourceApplication> findByClubIdInAndStatusOrderByCreatedAtDesc(java.util.Collection<Long> clubIds, String status);
    long countByStatus(String status);
    long countByClubIdInAndStatus(java.util.Collection<Long> clubIds, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ResourceApplication r WHERE r.id = :id")
    Optional<ResourceApplication> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM ResourceApplication r
            JOIN FETCH r.resource res
            WHERE r.activityId = :activityId
              AND res.type = 'VENUE'
            ORDER BY r.id ASC
            """)
    List<ResourceApplication> findVenueByActivityIdForUpdate(@Param("activityId") Long activityId);

    @Query("""
            SELECT DISTINCT r
            FROM ResourceApplication r
            JOIN FETCH r.resource res
            WHERE r.clubId = :clubId
              AND r.status = 'APPROVED'
              AND res.type = 'VENUE'
              AND r.endTime > :now
              AND (r.activityId IS NULL OR (:activityId IS NOT NULL AND r.activityId = :activityId))
            ORDER BY r.startTime ASC, r.id ASC
            """)
    List<ResourceApplication> findBindableVenueApplications(@Param("clubId") Long clubId,
                                                            @Param("now") LocalDateTime now,
                                                            @Param("activityId") Long activityId);

    @Query("SELECT r FROM ResourceApplication r WHERE r.resource.id = :resourceId " +
           "AND r.status = 'APPROVED' " +
           "AND ((r.startTime < :endTime AND r.endTime > :startTime))")
    List<ResourceApplication> findConflictingApplications(@Param("resourceId") Long resourceId,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM ResourceApplication r WHERE r.resource.id = :resourceId " +
           "AND r.status = 'APPROVED' " +
           "AND ((r.startTime < :endTime AND r.endTime > :startTime))")
    int sumApprovedQuantityInPeriod(@Param("resourceId") Long resourceId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);
}
