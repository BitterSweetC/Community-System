package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByClubIdAndStatus(Long clubId, String status);
    List<Notice> findByScopeAndStatus(String scope, String status);
    List<Notice> findByStatus(String status);

    Page<Notice> findByStatus(String status, Pageable pageable);
    Page<Notice> findByClubIdAndStatus(Long clubId, String status, Pageable pageable);

    @Query("""
            SELECT n FROM Notice n
            WHERE n.status = :status
              AND (:clubId IS NULL OR n.clubId = :clubId)
              AND (:title IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%')))
              AND (:startTime IS NULL OR n.publishedAt >= :startTime)
              AND (:endTime IS NULL OR n.publishedAt <= :endTime)
            """)
    Page<Notice> searchPublished(@Param("clubId") Long clubId,
                                 @Param("status") String status,
                                 @Param("title") String title,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime,
                                 Pageable pageable);
}
