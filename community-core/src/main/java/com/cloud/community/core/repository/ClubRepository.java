package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Club;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findByStatusAndDissolutionDateBefore(String status, LocalDateTime date);
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

    long countByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT c.status, COUNT(c) FROM Club c GROUP BY c.status")
    List<Object[]> countStatusDistribution();

    @Modifying
    @Query("UPDATE Club c SET c.visitCount = COALESCE(c.visitCount, 0) + 1 WHERE c.id = :id")
    void incrementVisitCount(@Param("id") Long id);

    List<Club> findByStatusAndCategoryIn(String status, Collection<String> categories);

    // Recommendation
    List<Club> findTop10ByStatusOrderByVisitCountDesc(String status);

    @Query("SELECT DISTINCT c FROM Club c LEFT JOIN c.tags t WHERE c.status = :status AND (c.category IN :interests OR t IN :interests)")
    List<Club> findByInterests(@Param("status") String status, @Param("interests") Collection<String> interests);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Club c WHERE c.id = :id")
    Optional<Club> findByIdForUpdate(@Param("id") Long id);
}
