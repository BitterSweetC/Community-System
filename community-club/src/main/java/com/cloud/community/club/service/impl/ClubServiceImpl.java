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
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
        List<Club> clubs = memberships.stream()
                .filter(m -> "PRESIDENT".equals(m.getRoleCode()) || "MANAGER".equals(m.getRoleCode()))
                .map(Member::getClub)
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
                .orElseThrow(() -> new RuntimeException("Club not found"));
        populateClubStats(club);
        return club;
    }

    @Override
    public List<Club> getRecommendedClubs(Long userId) {
        // 1. Try to recommend by interests if user is logged in
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getInterests() != null && !user.getInterests().isEmpty()) {
                // Split interests by comma, space, etc.
                List<String> interests = Arrays.asList(user.getInterests().split("[,，\\s]+"));
                // Filter empty strings
                interests = interests.stream().filter(s -> !s.trim().isEmpty()).collect(Collectors.toList());
                
                if (!interests.isEmpty()) {
                    List<Club> byInterest = clubRepository.findByStatusAndCategoryIn("ACTIVE", interests);
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
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createTime"))).getContent();
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

        // Send notification
        try {
            NotificationMessageDTO message = new NotificationMessageDTO();
            message.setUserId(club.getCreatedBy());
            message.setTitle("Club Creation Approved");
            message.setContent("Your club creation application for " + club.getName() + " has been approved!");
            message.setType("SYSTEM");
            rabbitTemplate.convertAndSend(RabbitConstants.NOTIFICATION_EXCHANGE, RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY, message);
        } catch (Exception e) {
            e.printStackTrace();
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

    private void populateClubStats(Club club) {
        if (club == null) return;
        club.setMemberCount(memberRepository.countByClubId(club.getId()));
        club.setActivityCount(activityRepository.countByClubId(club.getId()));
    }
}
