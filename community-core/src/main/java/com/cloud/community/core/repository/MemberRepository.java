package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByClubId(Long clubId);
    List<Member> findByUserId(Long userId);
    Optional<Member> findByClubIdAndUserId(Long clubId, Long userId);
    
    long countByClubId(Long clubId);

    @org.springframework.data.jpa.repository.Query("SELECT m.roleCode, COUNT(m) FROM Member m WHERE m.club.id = :clubId GROUP BY m.roleCode")
    List<Object[]> countRoleDistribution(@Param("clubId") Long clubId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT DATE_FORMAT(join_at, '%m-%d') as dateStr, COUNT(*) FROM t_member WHERE club_id = :clubId AND join_at >= :since GROUP BY dateStr", nativeQuery = true)
    List<Object[]> countMemberJoinTrend(@Param("clubId") Long clubId, @Param("since") java.time.LocalDateTime since);
}
