package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByClubIdAndStatus(Long clubId, String status);
    List<Notice> findByScopeAndStatus(String scope, String status);
    List<Notice> findByStatus(String status);

    Page<Notice> findByStatus(String status, Pageable pageable);
    Page<Notice> findByClubIdAndStatus(Long clubId, String status, Pageable pageable);
}
