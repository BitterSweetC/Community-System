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
                .orElseThrow(() -> new RuntimeException("未发现用户"));

        // 1. Global Admin check
        boolean isGlobalAdmin = user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getCode()));
        if (isGlobalAdmin) {
            return;
        }

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("未发现社团"));
        if (!Club.STATUS_ACTIVE.equals(club.getStatus())) {
            throw new AccessDeniedException("操作被拒绝: 社团未激活。");
        }

        // 2. Club Admin check
        Member member = memberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new AccessDeniedException("您不是该社团的成员"));

        if (!"ACTIVE".equals(member.getStatus())) {
            throw new AccessDeniedException("操作被拒绝: 您的成员资格未激活。");
        }

        if (!"PRESIDENT".equals(member.getRoleCode()) && !"MANAGER".equals(member.getRoleCode())) {
            throw new AccessDeniedException("操作被拒绝: 您必须是该社团的主席或管理员。");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkSystemAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("未发现用户"));

        boolean isGlobalAdmin = user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getCode()));
        if (!isGlobalAdmin) {
            throw new AccessDeniedException("操作被拒绝: 需要系统管理员权限。");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkClubActive(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("未发现社团"));

        if (Club.STATUS_DISSOLVED.equals(club.getStatus()) || Club.STATUS_DISSOLVING.equals(club.getStatus())) {
            throw new RuntimeException("操作被拒绝: 社团已解散或正在解散过程中。");
        }

        if (!Club.STATUS_ACTIVE.equals(club.getStatus())) {
            throw new RuntimeException("操作被拒绝: 社团未激活。");
        }
    }
}
