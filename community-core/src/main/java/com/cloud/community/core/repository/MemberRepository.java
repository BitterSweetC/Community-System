package com.cloud.community.core.repository;

import com.cloud.community.core.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByClubId(Long clubId);
    List<Member> findByUserId(Long userId);
    Optional<Member> findByClubIdAndUserId(Long clubId, Long userId);
}
