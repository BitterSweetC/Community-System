package com.cloud.community.core.repository;

import com.cloud.community.core.entity.ClubFinance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClubFinanceRepository extends JpaRepository<ClubFinance, Long> {
    List<ClubFinance> findByClubId(Long clubId);
    List<ClubFinance> findByClubIdAndStatus(Long clubId, String status);
    boolean existsByClubIdAndStatus(Long clubId, String status);
}
