package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findByStatus(String status);
    List<Club> findByCreatedBy(Long userId);
    List<Club> findByNameContainingIgnoreCase(String name);
    List<Club> findByCategoryAndStatus(String category, String status);
    List<Club> findByNameContainingIgnoreCaseAndCategoryAndStatus(String name, String category, String status);
    List<Club> findByNameContainingIgnoreCaseAndStatus(String name, String status);

    // Pagination Support
    org.springframework.data.domain.Page<Club> findByStatus(String status, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Club> findByCategoryAndStatus(String category, String status, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Club> findByNameContainingIgnoreCaseAndCategoryAndStatus(String name, String category, String status, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Club> findByNameContainingIgnoreCaseAndStatus(String name, String status, org.springframework.data.domain.Pageable pageable);
}
