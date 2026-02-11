package com.cloud.community.core.repository;

import com.cloud.community.core.entity.ClubFinance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClubFinanceRepository extends JpaRepository<ClubFinance, Long> {
    List<ClubFinance> findByClubId(Long clubId);
    List<ClubFinance> findByClubIdAndStatus(Long clubId, String status);
    boolean existsByClubIdAndStatus(Long clubId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM ClubFinance f WHERE f.id = :id")
    Optional<ClubFinance> findByIdForUpdate(@Param("id") Long id);
}
