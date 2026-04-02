package com.cloud.community.club.service.impl;

import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.Member;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.ClubRepository;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.club.service.ClubService;
import com.cloud.community.core.entity.Role;
import com.cloud.community.core.exception.BusinessException;
import com.cloud.community.core.repository.RoleRepository;
import com.cloud.community.notice.service.NotificationService;
import com.cloud.community.club.service.FinanceService;
import com.cloud.community.core.repository.ActivityRepository;
import com.cloud.community.core.model.dto.NotificationMessageDTO;
import com.cloud.community.core.constant.RabbitConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;

@Service
@RequiredArgsConstructor
public class ClubServiceImpl implements ClubService {

    private final ClubRepository clubRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FinanceService financeService;
    private final ActivityRepository activityRepository;
    private final RabbitTemplate rabbitTemplate;
    private final com.cloud.community.club.service.ChatService chatService;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public Club createClub(Club club, Long userId) {
        club.setCreatedBy(userId);
        club.setStatus("PENDING");
        Club savedClub = clubRepository.save(club);

        // Clean up legacy CLUB_ADMIN leakage for users who only submitted pending
        // clubs.
        checkAndRemoveClubAdminRole(userId);

        return savedClub;
    }

    @Override
    public List<Club> getAllClubs() {
        List<Club> clubs = clubRepository.findAll();
        populateClubStats(clubs);
        return clubs;
    }

    @Override
    public List<Club> getMyClubs(Long userId) {
        List<Member> memberships = memberRepository.findByUserId(userId);
        List<Club> clubs = memberships.stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .map(Member::getClub)
                .filter(c -> Club.STATUS_ACTIVE.equals(c.getStatus()))
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        clubs.forEach(this::populateClubStats);
        return clubs;
    }

    @Override
    @Transactional
    public Club getClubById(Long id) {
        clubRepository.incrementVisitCount(id);
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("未发现社团"));
        populateClubStats(club);
        return club;
    }

    @Override
    public List<Club> getRecommendedClubs(Long userId, String mode) {
        // 0. Try Collaborative Filtering via Python Agent
        try {
            List<Long> recommendedIds = chatService.getRecommendations(userId, mode);
            if (recommendedIds != null && !recommendedIds.isEmpty()) {
                List<Club> clubs = clubRepository.findAllById(recommendedIds);
                // Maintain order from recommendation
                java.util.Map<Long, Club> clubMap = clubs.stream().collect(Collectors.toMap(Club::getId, c -> c));
                List<Club> orderedClubs = new java.util.ArrayList<>();
                for (Long id : recommendedIds) {
                    if (clubMap.containsKey(id)) {
                        orderedClubs.add(clubMap.get(id));
                    }
                }
                if (!orderedClubs.isEmpty())
                    return orderedClubs;
            }
        } catch (Exception e) {
            // Continue to fallback strategies if AI recommendation fails
        }

        // 1. Try to recommend by interests if user is logged in
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getInterests() != null && !user.getInterests().isEmpty()) {
                // Split interests by comma, space, etc.
                List<String> interests = Arrays.asList(user.getInterests().split("[,\\uFF0C\\s]+"));
                // Filter empty strings
                interests = interests.stream().filter(s -> !s.trim().isEmpty()).collect(Collectors.toList());

                if (!interests.isEmpty()) {
                    List<Club> byInterest = clubRepository.findByInterests("ACTIVE", interests);
                    if (!byInterest.isEmpty()) {
                        return byInterest;
                    }
                }
            }
        }

        // 2. Fallback to popular (top 10 by visit count)
        List<Club> popular = clubRepository.findByStatus("ACTIVE",
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "visitCount"))).getContent();

        if (!popular.isEmpty()) {
            return popular;
        }

        // 3. Last resort: Just return newest active clubs
        return clubRepository.findByStatus("ACTIVE",
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    @Override
    @Transactional
    public Club updateClub(Long id, Club club) {
        Club existing = getClubById(id);
        existing.setName(club.getName());
        existing.setShortName(club.getShortName());
        existing.setDescription(club.getDescription());
        existing.setCategory(club.getCategory());
        existing.setLogoUrl(club.getLogoUrl());
        existing.setFoundedYear(club.getFoundedYear());
        if (club.getTags() != null) {
            if (existing.getTags() == null) {
                existing.setTags(new java.util.HashSet<>());
            }
            existing.getTags().clear();
            existing.getTags().addAll(club.getTags());
        }
        return clubRepository.save(existing);
    }

    @Override
    public List<Club> searchClubs(String keyword) {
        List<Club> clubs = clubRepository.findByNameContainingIgnoreCase(keyword);
        populateClubStats(clubs);
        return clubs;
    }

    @Override
    public List<Club> searchClubs(String keyword, String category) {
        List<Club> clubs;
        if (keyword != null && !keyword.isEmpty() && category != null && !category.isEmpty()) {
            clubs = clubRepository.findByNameContainingIgnoreCaseAndCategoryAndStatus(keyword, category, "ACTIVE");
        } else if (keyword != null && !keyword.isEmpty()) {
            clubs = clubRepository.findByNameContainingIgnoreCaseAndStatus(keyword, "ACTIVE");
        } else if (category != null && !category.isEmpty()) {
            clubs = clubRepository.findByCategoryAndStatus(category, "ACTIVE");
        } else {
            clubs = clubRepository.findByStatus("ACTIVE");
        }
        populateClubStats(clubs);
        return clubs;
    }

    @Override
    public org.springframework.data.domain.Page<Club> searchClubs(String keyword, String category, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Club> result;
        if (keyword != null && !keyword.isEmpty() && category != null && !category.isEmpty()) {
            result = clubRepository.findByNameContainingIgnoreCaseAndCategoryAndStatus(keyword, category, "ACTIVE",
                    pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            result = clubRepository.findByNameContainingIgnoreCaseAndStatus(keyword, "ACTIVE", pageable);
        } else if (category != null && !category.isEmpty()) {
            result = clubRepository.findByCategoryAndStatus(category, "ACTIVE", pageable);
        } else {
            result = clubRepository.findByStatus("ACTIVE", pageable);
        }
        populateClubStats(result.getContent());
        return result;
    }

    @Override
    public List<Club> getPendingClubs() {
        return clubRepository.findByStatus("PENDING");
    }

    @Override
    public org.springframework.data.domain.Page<Club> getPendingClubs(int page, int size) {
        return clubRepository.findByStatus("PENDING",
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Override
    public List<Club> getDissolvingClubs() {
        return clubRepository.findByStatus("DISSOLVING");
    }

    @Override
    public org.springframework.data.domain.Page<Club> getDissolvingClubs(int page, int size) {
        return clubRepository.findByStatus("DISSOLVING",
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Override
    @Transactional
    public void deleteClub(Long id, Long adminId) {
        forceDissolve(id, adminId, "Deleted via API");
    }

    @Override
    @Transactional
    public void approveClub(Long clubId) {
        Club club = clubRepository.findByIdForUpdate(clubId)
                .orElseThrow(() -> new RuntimeException("未找到社团"));
        club.setStatus(Club.STATUS_ACTIVE);
        clubRepository.save(club);

        ensureCreatorMembershipAfterApproval(club);

        // Grant CLUB_ADMIN only after the club becomes ACTIVE.
        List<Member> members = memberRepository.findByClubId(clubId);
        members.stream()
                .filter(m -> "MANAGER".equals(m.getRoleCode()) || "PRESIDENT".equals(m.getRoleCode()))
                .forEach(m -> grantClubAdminRole(m.getUser()));

        // Send notification
        try {
            NotificationMessageDTO message = new NotificationMessageDTO();
            message.setUserId(club.getCreatedBy());
            message.setTitle("社团创建审批通过");
            message.setContent("您的社团 " + club.getName() + " 已通过审批！");
            message.setType("SYSTEM");
            rabbitTemplate.convertAndSend(RabbitConstants.NOTIFICATION_EXCHANGE,
                    RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public void addMember(Long clubId, Long userId, String role) {
        // Lock club to serialize member additions
        Club club = clubRepository.findByIdForUpdate(clubId)
                .orElseThrow(() -> new RuntimeException("未找到社团"));

        if (memberRepository.findByClubIdAndUserId(clubId, userId).isPresent()) {
            throw new BusinessException(40921, "该用户已是社团成员，请勿重复添加");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("未找到用户"));

        Member member = new Member();
        member.setClub(club);
        member.setUser(user);
        member.setRoleCode(role);
        member.setJoinAt(LocalDateTime.now());
        member.setStatus("ACTIVE");

        memberRepository.save(member);

        // Grant CLUB_ADMIN for PRESIDENT/MANAGER roles.
        if ("MANAGER".equals(role) || "PRESIDENT".equals(role)) {
            grantClubAdminRole(user);
        }
    }

    @Override
    public List<Member> getClubMembers(Long clubId) {
        return memberRepository.findByClubId(clubId);
    }

    @Override
    public Page<Member> getClubMembers(Long clubId, int page, int size) {
        return memberRepository.findByClubIdOrderByJoinAtDesc(clubId, PageRequest.of(page, size));
    }

    private void ensureCreatorMembershipAfterApproval(Club club) {
        if (club == null || club.getId() == null || club.getCreatedBy() == null) {
            return;
        }

        User creator = userRepository.findById(club.getCreatedBy())
                .orElseThrow(() -> new RuntimeException("未找到用户"));

        Member creatorMember = memberRepository.findByClubIdAndUserIdForUpdate(club.getId(), creator.getId())
                .orElse(null);

        if (creatorMember == null) {
            creatorMember = new Member();
            creatorMember.setClub(club);
            creatorMember.setUser(creator);
            creatorMember.setJoinAt(LocalDateTime.now());
        }

        creatorMember.setClub(club);
        creatorMember.setUser(creator);
        creatorMember.setRoleCode("PRESIDENT");
        creatorMember.setStatus("ACTIVE");
        if (creatorMember.getJoinAt() == null) {
            creatorMember.setJoinAt(LocalDateTime.now());
        }
        memberRepository.save(creatorMember);
    }

    private static final Set<String> VALID_ROLES = Set.of("PRESIDENT", "MANAGER", "MEMBER");

    @Override
    @Transactional
    public void updateMemberRole(Long clubId, Long userId, String role) {
        if (role == null || !VALID_ROLES.contains(role)) {
            throw new RuntimeException("Invalid role. Allowed values: " + VALID_ROLES);
        }
        Member member = memberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new RuntimeException("未找到成员"));
        member.setRoleCode(role);
        memberRepository.save(member);

        // Grant CLUB_ADMIN for PRESIDENT/MANAGER roles.
        if ("MANAGER".equals(role) || "PRESIDENT".equals(role)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("未找到用户"));
            grantClubAdminRole(user);
        } else if ("MEMBER".equals(role)) {
            // Re-check whether CLUB_ADMIN should be removed.
            checkAndRemoveClubAdminRole(userId);
        }
    }

    @Override
    @Transactional
    public void removeMember(Long clubId, Long userId) {
        Member member = memberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new RuntimeException("未找到成员"));
        // SRS FR-MEMBER-05: status change record traceable. We mark as LEFT/REMOVED.
        member.setStatus("LEFT");
        memberRepository.save(member);

        // Re-check whether CLUB_ADMIN should be removed.
        checkAndRemoveClubAdminRole(userId);
    }

    @Override
    @Transactional
    public void applyDissolution(Long clubId, Long userId, String reason) {
        // Lock club to ensure consistent state check
        Club club = clubRepository.findByIdForUpdate(clubId)
                .orElseThrow(() -> new RuntimeException("未找到社团"));

        if (financeService.hasPendingTransactions(clubId)) {
            throw new RuntimeException("无法解散：存在未结算的财务交易");
        }

        // Check for active activities
        // Assuming statuses: PUBLISHED, ONGOING, SIGNUP. Adjust as per actual Activity
        // statuses if known.
        // Based on common sense, these are likely candidates.
        if (activityRepository.existsByClubIdAndStatusIn(clubId, Arrays.asList("PUBLISHED", "ONGOING", "SIGNUP"))) {
            throw new RuntimeException("无法解散：存在进行中的活动");
        }

        club.setStatus(Club.STATUS_DISSOLVING);
        club.setDissolutionReason(reason);
        club.setDissolutionDate(LocalDateTime.now());
        clubRepository.save(club);

        notificationService.notifyClubMembers(clubId, "社团解散通知",
                "社团进入解散冷却期. 原因: " + reason);
    }

    @Override
    @Transactional
    public void withdrawDissolution(Long clubId, Long userId) {
        Club club = clubRepository.findByIdForUpdate(clubId)
                .orElseThrow(() -> new RuntimeException("未找到社团"));
        if (!Club.STATUS_DISSOLVING.equals(club.getStatus())) {
            throw new BusinessException(40922, "社团当前不在解散流程中");
        }

        club.setStatus(Club.STATUS_ACTIVE);
        club.setDissolutionReason(null);
        club.setDissolutionDate(null);
        clubRepository.save(club);

        notificationService.notifyClubMembers(clubId, "解散申请已撤回",
                "解散申请已撤回。");
    }

    @Override
    @Transactional
    public void approveDissolution(Long clubId, Long adminId) {
        Club club = clubRepository.findByIdForUpdate(clubId)
                .orElseThrow(() -> new RuntimeException("未找到社团"));
        if (!Club.STATUS_DISSOLVING.equals(club.getStatus())) {
            throw new BusinessException(40922, "社团当前不在解散流程中");
        }
        club.setStatus(Club.STATUS_DISSOLVED);
        if (club.getDissolutionDate() == null) {
            club.setDissolutionDate(LocalDateTime.now());
        }
        clubRepository.save(club);

        // Re-check managers and presidents after dissolution/recovery.
        List<Member> members = memberRepository.findByClubId(clubId);
        members.stream()
                .filter(m -> "MANAGER".equals(m.getRoleCode()) || "PRESIDENT".equals(m.getRoleCode()))
                .forEach(m -> checkAndRemoveClubAdminRole(m.getUser().getId()));
    }

    @Override
    @Transactional
    public void rejectDissolution(Long clubId, Long adminId) {
        Club club = clubRepository.findByIdForUpdate(clubId)
                .orElseThrow(() -> new RuntimeException("未找到社团"));
        if (!Club.STATUS_DISSOLVING.equals(club.getStatus())) {
            throw new BusinessException(40922, "社团当前不在解散流程中");
        }
        club.setStatus(Club.STATUS_ACTIVE);
        club.setDissolutionReason(null);
        club.setDissolutionDate(null);
        clubRepository.save(club);
    }

    @Override
    @Transactional
    public void forceDissolve(Long clubId, Long adminId, String reason) {
        Club club = clubRepository.findByIdForUpdate(clubId)
                .orElseThrow(() -> new RuntimeException("未找到社团"));
        club.setStatus(Club.STATUS_DISSOLVED);
        club.setDissolutionReason("Forced by Admin: " + reason);
        club.setDissolutionDate(LocalDateTime.now());
        clubRepository.save(club);

        notificationService.notifyClubMembers(clubId, "社团已解散",
                "社团已被系统管理员解散。原因: " + reason);

        // Re-check managers and presidents after dissolution/recovery.
        List<Member> members = memberRepository.findByClubId(clubId);
        members.stream()
                .filter(m -> "MANAGER".equals(m.getRoleCode()) || "PRESIDENT".equals(m.getRoleCode()))
                .forEach(m -> checkAndRemoveClubAdminRole(m.getUser().getId()));
    }

    @Override
    @Transactional
    public void recoverClub(Long clubId, Long adminId) {
        Club club = clubRepository.findByIdForUpdate(clubId)
                .orElseThrow(() -> new RuntimeException("未找到社团"));
        if (!Club.STATUS_DISSOLVED.equals(club.getStatus())) {
            throw new BusinessException(40923, "社团当前不是已解散状态，无法恢复");
        }
        club.setStatus(Club.STATUS_ACTIVE);
        club.setDissolutionReason(null);
        club.setDissolutionDate(null);
        clubRepository.save(club);

        // Re-check managers and presidents after dissolution/recovery.
        List<Member> members = memberRepository.findByClubId(clubId);
        members.stream()
                .filter(m -> "MANAGER".equals(m.getRoleCode()) || "PRESIDENT".equals(m.getRoleCode()))
                .forEach(m -> grantClubAdminRole(m.getUser()));
    }

    // Preserve USER role when promoting to CLUB_ADMIN so users retain student
    // capabilities.
    private void grantClubAdminRole(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        User lockedUser = userRepository.findByIdForUpdate(user.getId()).orElse(null);
        if (lockedUser == null) {
            return;
        }
        if (lockedUser.getRoles() == null) {
            lockedUser.setRoles(new java.util.HashSet<>());
        }

        boolean changed = false;
        Role clubAdminRole = roleRepository.findByCode("CLUB_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role CLUB_ADMIN not found"));
        if (!lockedUser.getRoles().contains(clubAdminRole)) {
            lockedUser.getRoles().add(clubAdminRole);
            changed = true;
        }

        if (changed) {
            userRepository.save(lockedUser);
        }
    }

    // Remove CLUB_ADMIN when user is no longer admin in any ACTIVE club.
    private void checkAndRemoveClubAdminRole(Long userId) {
        List<Member> memberships = memberRepository.findByUserId(userId);
        boolean isAdminInAnyActiveClub = memberships.stream()
                .anyMatch(m -> "ACTIVE".equals(m.getStatus())
                        && m.getClub() != null
                        && Club.STATUS_ACTIVE.equals(m.getClub().getStatus())
                        && ("MANAGER".equals(m.getRoleCode()) || "PRESIDENT".equals(m.getRoleCode())));

        if (!isAdminInAnyActiveClub) {
            User user = userRepository.findByIdForUpdate(userId).orElse(null);
            if (user != null) {
                boolean changed = false;
                Role clubAdminRole = roleRepository.findByCode("CLUB_ADMIN").orElse(null);
                if (clubAdminRole != null && user.getRoles().contains(clubAdminRole)) {
                    user.getRoles().remove(clubAdminRole);
                    changed = true;
                }

                // Ensure users keep the default student role instead of ending up with no role.
                if (user.getRoles() == null || user.getRoles().isEmpty()) {
                    Role userRole = roleRepository.findByCode("USER").orElse(null);
                    if (userRole != null) {
                        if (user.getRoles() == null) {
                            user.setRoles(new java.util.HashSet<>());
                        }
                        user.getRoles().add(userRole);
                        changed = true;
                    }
                }

                if (changed) {
                    userRepository.save(user);
                }
            }
        }
    }

    private void populateClubStats(Club club) {
        if (club == null)
            return;
        List<Member> members = memberRepository.findByClubId(club.getId());
        club.setMemberCount(members.stream()
                .filter(member -> "ACTIVE".equals(member.getStatus()))
                .count());
        club.setActivityCount(activityRepository.countByClubId(club.getId()));
        club.setPresidentName(members.stream()
                .filter(member -> "ACTIVE".equals(member.getStatus()))
                .filter(member -> "PRESIDENT".equals(member.getRoleCode()))
                .map(Member::getUser)
                .filter(java.util.Objects::nonNull)
                .map(user -> user.getRealName() != null && !user.getRealName().isBlank() ? user.getRealName()
                        : user.getUsername())
                .findFirst()
                .orElse(null));
    }

    private void populateClubStats(List<Club> clubs) {
        if (clubs == null || clubs.isEmpty()) {
            return;
        }
        clubs.forEach(this::populateClubStats);
    }

    @Override
    @Transactional
    public int cleanupOrphanedClubAdminRoles() {
        Role clubAdminRole = roleRepository.findByCode("CLUB_ADMIN")
                .orElseThrow(() -> new RuntimeException("未找到角色社团管理员"));
        Role userRole = roleRepository.findByCode("USER").orElse(null);

        List<User> usersWithClubAdmin = userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(clubAdminRole))
                .collect(java.util.stream.Collectors.toList());

        int count = 0;
        for (User user : usersWithClubAdmin) {
            checkAndRemoveClubAdminRole(user.getId());
            User refreshedUser = userRepository.findByIdForUpdate(user.getId()).orElse(null);
            if (refreshedUser != null) {
                if (refreshedUser.getRoles() != null && !refreshedUser.getRoles().contains(clubAdminRole)) {
                    count++;
                } else if (userRole != null
                        && refreshedUser.getRoles() != null
                        && refreshedUser.getRoles().contains(clubAdminRole)
                        && !refreshedUser.getRoles().contains(userRole)) {
                    refreshedUser.getRoles().add(userRole);
                    userRepository.save(refreshedUser);
                    count++;
                }
            }
        }

        // Backfill default student role for legacy users that currently have no role.
        if (userRole != null) {
            List<User> usersWithoutRoles = userRepository.findAll().stream()
                    .filter(u -> u.getRoles() == null || u.getRoles().isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            for (User user : usersWithoutRoles) {
                User lockedUser = userRepository.findByIdForUpdate(user.getId()).orElse(null);
                if (lockedUser == null) {
                    continue;
                }
                if (lockedUser.getRoles() == null || lockedUser.getRoles().isEmpty()) {
                    if (lockedUser.getRoles() == null) {
                        lockedUser.setRoles(new java.util.HashSet<>());
                    }
                    lockedUser.getRoles().add(userRole);
                    userRepository.save(lockedUser);
                    count++;
                }
            }
        }

        return count;
    }
}
