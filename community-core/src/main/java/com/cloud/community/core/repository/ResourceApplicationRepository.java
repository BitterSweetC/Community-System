package com.cloud.community.core.repository;

import com.cloud.community.core.entity.ResourceApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceApplicationRepository extends JpaRepository<ResourceApplication, Long> {
    List<ResourceApplication> findByClubId(Long clubId);
    List<ResourceApplication> findByStatus(String status);

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
