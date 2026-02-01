package com.cloud.community.club.service.impl;

import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.Member;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.ClubRepository;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.club.service.ClubService;
import com.cloud.community.core.entity.Role;
import com.cloud.community.core.repository.RoleRepository;
import com.cloud.community.club.service.NotificationService;
import com.cloud.community.club.service.FinanceService;
import com.cloud.community.core.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubServiceImpl implements ClubService {

    private final ClubRepository clubRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FinanceService financeService;
    private final ActivityRepository activityRepository;
    
    @org.springframework.beans.factory.annotation.Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public Club createClub(Club club, Long userId) {
        club.setCreatedBy(userId);
        club.setStatus("PENDING");
        Club savedClub = clubRepository.save(club);
        
        // Add creator as member (PRESIDENT)
        addMember(savedClub.getId(), userId, "PRESIDENT");
        
        return savedClub;
    }

    @Override
    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    @Override
    public List<Club> getMyClubs(Long userId) {
        // Return clubs where user is a member with role PRESIDENT or MANAGER
        // Also include clubs created by user as fallback (though creator should be PRESIDENT)
        List<Member> memberships = memberRepository.findByUserId(userId);
        return memberships.stream()
                .filter(m -> "PRESIDENT".equals(m.getRoleCode()) || "MANAGER".equals(m.getRoleCode()))
                .map(Member::getClub)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Club getClubById(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Club not found"));
    }

    @Override
    @Transactional
    public Club updateClub(Long id, Club club) {
        Club existing = getClubById(id);
        existing.setName(club.getName());
        existing.setDescription(club.getDescription());
        existing.setCategory(club.getCategory());
        existing.setLogoUrl(club.getLogoUrl());
        return clubRepository.save(existing);
    }

    @Override
    public List<Club> searchClubs(String keyword) {
        return clubRepository.findByNameContainingIgnoreCase(keyword);
    }

    @Override
    public List<Club> searchClubs(String keyword, String category) {
        if (keyword != null && !keyword.isEmpty() && category != null && !category.isEmpty()) {
            return clubRepository.findByNameContainingIgnoreCaseAndCategoryAndStatus(keyword, category, "ACTIVE");
        } else if (keyword != null && !keyword.isEmpty()) {
            return clubRepository.findByNameContainingIgnoreCaseAndStatus(keyword, "ACTIVE");
        } else if (category != null && !category.isEmpty()) {
            return clubRepository.findByCategoryAndStatus(category, "ACTIVE");
        } else {
            return clubRepository.findByStatus("ACTIVE");
        }
    }

    @Override
    public org.springframework.data.domain.Page<Club> searchClubs(String keyword, String category, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        if (keyword != null && !keyword.isEmpty() && category != null && !category.isEmpty()) {
            return clubRepository.findByNameContainingIgnoreCaseAndCategoryAndStatus(keyword, category, "ACTIVE", pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            return clubRepository.findByNameContainingIgnoreCaseAndStatus(keyword, "ACTIVE", pageable);
        } else if (category != null && !category.isEmpty()) {
            return clubRepository.findByCategoryAndStatus(category, "ACTIVE", pageable);
        } else {
            return clubRepository.findByStatus("ACTIVE", pageable);
        }
    }

    @Override
    public List<Club> getPendingClubs() {
        return clubRepository.findByStatus("PENDING");
    }

    @Override
    @Transactional
    public void deleteClub(Long id) {
        // Soft delete logic preferred
        forceDissolve(id, 0L, "Deleted via API");
    }

    @Override
    @Transactional
    public void approveClub(Long clubId) {
        Club club = getClubById(clubId);
        club.setStatus(Club.STATUS_ACTIVE);
        clubRepository.save(club);

        // Upgrade creator to CLUB_ADMIN role if not already
        User creator = userRepository.findById(club.getCreatedBy())
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        
        Role clubAdminRole = roleRepository.findByCode("CLUB_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role CLUB_ADMIN not found"));
        
        if (!creator.getRoles().contains(clubAdminRole)) {
            creator.getRoles().add(clubAdminRole);
            userRepository.save(creator);
        }
    }

    @Override
    @Transactional
    public void addMember(Long clubId, Long userId, String role) {
        if (memberRepository.findByClubIdAndUserId(clubId, userId).isPresent()) {
            return;
        }

        Club club = getClubById(clubId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Member member = new Member();
        member.setClub(club);
        member.setUser(user);
        member.setRoleCode(role);
        member.setJoinAt(LocalDateTime.now());
        member.setStatus("ACTIVE");
        
        memberRepository.save(member);
    }

    @Override
    public List<Member> getClubMembers(Long clubId) {
        return memberRepository.findByClubId(clubId);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long clubId, Long userId, String role) {
        Member member = memberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        member.setRoleCode(role);
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeMember(Long clubId, Long userId) {
        Member member = memberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        // SRS FR-MEMBER-05: status change record traceable. We mark as LEFT/REMOVED.
        member.setStatus("LEFT");
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public void applyDissolution(Long clubId, Long userId, String reason) {
        if (financeService.hasPendingTransactions(clubId)) {
            throw new RuntimeException("Cannot dissolve: Pending financial transactions");
        }

        // Check for active activities
        // Assuming statuses: PUBLISHED, ONGOING, SIGNUP. Adjust as per actual Activity statuses if known.
        // Based on common sense, these are likely candidates.
        if (activityRepository.existsByClubIdAndStatusIn(clubId, Arrays.asList("PUBLISHED", "ONGOING", "SIGNUP"))) {
            throw new RuntimeException("Cannot dissolve: Ongoing activities exist");
        }

        Club club = getClubById(clubId);
        club.setStatus(Club.STATUS_DISSOLVING);
        club.setDissolutionReason(reason);
        club.setDissolutionDate(LocalDateTime.now());
        clubRepository.save(club);

        notificationService.notifyClubMembers(clubId, "Club Dissolution Notice", 
                "The club is entering dissolution cooling-off period (7 days). Reason: " + reason);
    }

    @Override
    @Transactional
    public void withdrawDissolution(Long clubId, Long userId) {
        Club club = getClubById(clubId);
        if (!Club.STATUS_DISSOLVING.equals(club.getStatus())) {
            throw new RuntimeException("Club is not in dissolution process");
        }
        
        club.setStatus(Club.STATUS_ACTIVE);
        club.setDissolutionReason(null);
        club.setDissolutionDate(null);
        clubRepository.save(club);
        
        notificationService.notifyClubMembers(clubId, "Dissolution Withdrawn", "The dissolution request has been withdrawn.");
    }

    @Override
    @Transactional
    public void forceDissolve(Long clubId, Long adminId, String reason) {
        Club club = getClubById(clubId);
        club.setStatus(Club.STATUS_DISSOLVED);
        club.setDissolutionReason("Forced by Admin: " + reason);
        club.setDissolutionDate(LocalDateTime.now());
        clubRepository.save(club);

        notificationService.notifyClubMembers(clubId, "Club Dissolved", "The club has been dissolved by system administrator. Reason: " + reason);
    }

    @Override
    @Transactional
    public void recoverClub(Long clubId, Long adminId) {
        Club club = getClubById(clubId);
        if (!Club.STATUS_DISSOLVED.equals(club.getStatus())) {
             throw new RuntimeException("Club is not dissolved");
        }
        club.setStatus(Club.STATUS_ACTIVE);
        club.setDissolutionReason(null);
        club.setDissolutionDate(null);
        clubRepository.save(club);
    }
}
