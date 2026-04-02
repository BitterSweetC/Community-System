package com.cloud.community.club.service.impl;

import com.cloud.community.club.service.ChatService;
import com.cloud.community.club.service.FinanceService;
import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.Member;
import com.cloud.community.core.entity.Role;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.ActivityRepository;
import com.cloud.community.core.repository.ClubRepository;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.core.repository.RoleRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.notice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubServiceImplTest {

    @Mock ClubRepository clubRepository;
    @Mock MemberRepository memberRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock FinanceService financeService;
    @Mock ActivityRepository activityRepository;
    @Mock RabbitTemplate rabbitTemplate;
    @Mock ChatService chatService;
    @Mock RoleRepository roleRepository;

    @InjectMocks ClubServiceImpl clubService;

    @Test
    void createClub_keepsApplicantAsStudentUntilApproval() {
        Long userId = 100L;

        Club toCreate = new Club();
        toCreate.setName("Pending Club");

        Club savedClub = new Club();
        savedClub.setId(10L);
        savedClub.setCreatedBy(userId);
        savedClub.setStatus(Club.STATUS_PENDING);

        Role userRole = new Role();
        userRole.setCode("USER");
        Role clubAdminRole = new Role();
        clubAdminRole.setCode("CLUB_ADMIN");

        User applicant = new User();
        applicant.setId(userId);
        applicant.setRoles(new HashSet<>(List.of(userRole)));

        when(clubRepository.save(toCreate)).thenReturn(savedClub);
        when(memberRepository.findByUserId(userId)).thenReturn(List.of());
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(applicant));
        when(roleRepository.findByCode("CLUB_ADMIN")).thenReturn(Optional.of(clubAdminRole));

        Club result = clubService.createClub(toCreate, userId);

        assertThat(result.getStatus()).isEqualTo(Club.STATUS_PENDING);
        assertThat(applicant.getRoles()).containsExactly(userRole);
        verify(memberRepository, never()).save(any(Member.class));
        verify(userRepository, never()).save(applicant);
    }

    @Test
    void createClub_removesOrphanedClubAdminRole_whenApplicantHasNoActiveManagedClub() {
        Long userId = 100L;

        Club toCreate = new Club();
        Club savedClub = new Club();
        savedClub.setId(20L);
        savedClub.setCreatedBy(userId);
        savedClub.setStatus(Club.STATUS_PENDING);

        Role userRole = new Role();
        userRole.setCode("USER");
        Role clubAdminRole = new Role();
        clubAdminRole.setCode("CLUB_ADMIN");

        User applicant = new User();
        applicant.setId(userId);
        applicant.setRoles(new HashSet<>(List.of(userRole, clubAdminRole)));

        Club pendingClub = new Club();
        pendingClub.setId(88L);
        pendingClub.setStatus(Club.STATUS_PENDING);

        Member pendingPresident = new Member();
        pendingPresident.setClub(pendingClub);
        pendingPresident.setUser(applicant);
        pendingPresident.setRoleCode("PRESIDENT");
        pendingPresident.setStatus("ACTIVE");

        when(clubRepository.save(toCreate)).thenReturn(savedClub);
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(pendingPresident));
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(applicant));
        when(roleRepository.findByCode("CLUB_ADMIN")).thenReturn(Optional.of(clubAdminRole));

        clubService.createClub(toCreate, userId);

        assertThat(applicant.getRoles()).contains(userRole);
        assertThat(applicant.getRoles()).doesNotContain(clubAdminRole);
        verify(userRepository).save(applicant);
    }

    @Test
    void forceDissolve_removesClubAdminRole_whenUserNoLongerAdminInAnyActiveClub() {
        Long clubId = 1L;
        Long userId = 100L;

        Club dissolvedClub = new Club();
        dissolvedClub.setId(clubId);
        dissolvedClub.setStatus(Club.STATUS_ACTIVE);

        Role clubAdminRole = new Role();
        clubAdminRole.setCode("CLUB_ADMIN");
        Role userRole = new Role();
        userRole.setCode("USER");

        User user = new User();
        user.setId(userId);
        user.setRoles(new HashSet<>(List.of(clubAdminRole)));

        Member member = new Member();
        member.setClub(dissolvedClub);
        member.setUser(user);
        member.setRoleCode("PRESIDENT");
        member.setStatus("ACTIVE");

        when(clubRepository.findByIdForUpdate(clubId)).thenReturn(Optional.of(dissolvedClub));
        when(memberRepository.findByClubId(clubId)).thenReturn(List.of(member));
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(member));
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
        when(roleRepository.findByCode("CLUB_ADMIN")).thenReturn(Optional.of(clubAdminRole));
        when(clubRepository.save(dissolvedClub)).thenReturn(dissolvedClub);

        clubService.forceDissolve(clubId, 999L, "Deleted via API");

        assertThat(user.getRoles()).doesNotContain(clubAdminRole);
        assertThat(user.getRoles()).contains(userRole);
        verify(userRepository).save(user);
    }

    @Test
    void forceDissolve_keepsClubAdminRole_whenUserStillAdminInAnotherActiveClub() {
        Long clubId = 1L;
        Long userId = 100L;

        Club dissolvedClub = new Club();
        dissolvedClub.setId(clubId);
        dissolvedClub.setStatus(Club.STATUS_ACTIVE);

        Club activeClub = new Club();
        activeClub.setId(2L);
        activeClub.setStatus(Club.STATUS_ACTIVE);

        Role clubAdminRole = new Role();
        clubAdminRole.setCode("CLUB_ADMIN");

        User user = new User();
        user.setId(userId);
        user.setRoles(new HashSet<>(List.of(clubAdminRole)));

        Member dissolvedClubMember = new Member();
        dissolvedClubMember.setClub(dissolvedClub);
        dissolvedClubMember.setUser(user);
        dissolvedClubMember.setRoleCode("PRESIDENT");
        dissolvedClubMember.setStatus("ACTIVE");

        Member activeClubMember = new Member();
        activeClubMember.setClub(activeClub);
        activeClubMember.setUser(user);
        activeClubMember.setRoleCode("MANAGER");
        activeClubMember.setStatus("ACTIVE");

        when(clubRepository.findByIdForUpdate(clubId)).thenReturn(Optional.of(dissolvedClub));
        when(memberRepository.findByClubId(clubId)).thenReturn(List.of(dissolvedClubMember));
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(dissolvedClubMember, activeClubMember));
        when(clubRepository.save(dissolvedClub)).thenReturn(dissolvedClub);

        clubService.forceDissolve(clubId, 999L, "Deleted via API");

        assertThat(user.getRoles()).contains(clubAdminRole);
        verify(userRepository, never()).save(user);
        verify(roleRepository, never()).findByCode("CLUB_ADMIN");
    }

    @Test
    void approveClub_promotesStudentToClubAdminOnly() {
        Long clubId = 10L;

        Club club = new Club();
        club.setId(clubId);
        club.setStatus(Club.STATUS_PENDING);
        club.setCreatedBy(100L);
        club.setName("Test Club");

        Role userRole = new Role();
        userRole.setCode("USER");
        Role clubAdminRole = new Role();
        clubAdminRole.setCode("CLUB_ADMIN");

        User user = new User();
        user.setId(100L);
        user.setRoles(new HashSet<>(List.of(userRole)));

        AtomicReference<Member> savedMember = new AtomicReference<>();

        when(clubRepository.findByIdForUpdate(clubId)).thenReturn(Optional.of(club));
        when(clubRepository.save(club)).thenReturn(club);
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(memberRepository.findByClubIdAndUserIdForUpdate(clubId, 100L)).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            savedMember.set(member);
            return member;
        });
        when(memberRepository.findByClubId(clubId)).thenAnswer(invocation ->
                savedMember.get() == null ? List.of() : new ArrayList<>(List.of(savedMember.get())));
        when(userRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("CLUB_ADMIN")).thenReturn(Optional.of(clubAdminRole));

        clubService.approveClub(clubId);

        assertThat(savedMember.get()).isNotNull();
        assertThat(savedMember.get().getRoleCode()).isEqualTo("PRESIDENT");
        assertThat(savedMember.get().getStatus()).isEqualTo("ACTIVE");
        assertThat(savedMember.get().getJoinAt()).isNotNull();
        assertThat(user.getRoles()).contains(clubAdminRole);
        assertThat(user.getRoles()).contains(userRole);
        verify(userRepository).save(user);
    }

    @Test
    void approveClub_reusesExistingCreatorMembershipWithoutDuplicating() {
        Long clubId = 11L;

        Club club = new Club();
        club.setId(clubId);
        club.setStatus(Club.STATUS_PENDING);
        club.setCreatedBy(100L);
        club.setName("Legacy Pending Club");

        Role userRole = new Role();
        userRole.setCode("USER");
        Role clubAdminRole = new Role();
        clubAdminRole.setCode("CLUB_ADMIN");

        User user = new User();
        user.setId(100L);
        user.setRoles(new HashSet<>(List.of(userRole)));

        Member existingMember = new Member();
        existingMember.setClub(club);
        existingMember.setUser(user);
        existingMember.setRoleCode("MEMBER");
        existingMember.setStatus("LEFT");

        when(clubRepository.findByIdForUpdate(clubId)).thenReturn(Optional.of(club));
        when(clubRepository.save(club)).thenReturn(club);
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(memberRepository.findByClubIdAndUserIdForUpdate(clubId, 100L)).thenReturn(Optional.of(existingMember));
        when(memberRepository.save(existingMember)).thenReturn(existingMember);
        when(memberRepository.findByClubId(clubId)).thenReturn(List.of(existingMember));
        when(userRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("CLUB_ADMIN")).thenReturn(Optional.of(clubAdminRole));

        clubService.approveClub(clubId);

        assertThat(existingMember.getRoleCode()).isEqualTo("PRESIDENT");
        assertThat(existingMember.getStatus()).isEqualTo("ACTIVE");
        verify(memberRepository, times(1)).save(existingMember);
        verify(userRepository).save(user);
    }
}

