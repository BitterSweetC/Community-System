package com.cloud.community.user.service.impl;

import com.cloud.community.core.entity.Club;
import com.cloud.community.core.repository.ClubRepository;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.core.entity.Member;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;

    @Override
    @Transactional(readOnly = true)
    public void checkClubAdmin(Long userId, Long clubId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Global Admin check
        boolean isGlobalAdmin = user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getCode()));
        if (isGlobalAdmin) {
            return;
        }

        // 2. Club Admin check
        Member member = memberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this club"));

        if (!"PRESIDENT".equals(member.getRoleCode()) && !"MANAGER".equals(member.getRoleCode())) {
            throw new AccessDeniedException("Insufficient permissions: You must be a PRESIDENT or MANAGER of this club.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkSystemAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        boolean isGlobalAdmin = user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getCode()));
        if (!isGlobalAdmin) {
            throw new AccessDeniedException("Insufficient permissions: System Admin required.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkClubActive(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));
        
        if (Club.STATUS_DISSOLVED.equals(club.getStatus()) || Club.STATUS_DISSOLVING.equals(club.getStatus())) {
            throw new RuntimeException("Operation denied: Club is dissolved or in dissolution process.");
        }
        
        if (!Club.STATUS_ACTIVE.equals(club.getStatus())) {
            throw new RuntimeException("Operation denied: Club is not active.");
        }
    }
}
