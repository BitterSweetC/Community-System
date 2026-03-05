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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
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

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(dissolvedClub));
        when(memberRepository.findByClubId(clubId)).thenReturn(List.of(member));
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(member));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("CLUB_ADMIN")).thenReturn(Optional.of(clubAdminRole));
        when(roleRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
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

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(dissolvedClub));
        when(memberRepository.findByClubId(clubId)).thenReturn(List.of(dissolvedClubMember));
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(dissolvedClubMember, activeClubMember));
        when(clubRepository.save(dissolvedClub)).thenReturn(dissolvedClub);

        clubService.forceDissolve(clubId, 999L, "Deleted via API");

        assertThat(user.getRoles()).contains(clubAdminRole);
        verify(userRepository, never()).save(user);
        verify(roleRepository, never()).findByCode("CLUB_ADMIN");
        verify(clubRepository).incrementVisitCount(anyLong());
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

        Member president = new Member();
        president.setClub(club);
        president.setUser(user);
        president.setRoleCode("PRESIDENT");
        president.setStatus("ACTIVE");

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(clubRepository.save(club)).thenReturn(club);
        when(memberRepository.findByClubId(clubId)).thenReturn(List.of(president));
        when(roleRepository.findByCode("CLUB_ADMIN")).thenReturn(Optional.of(clubAdminRole));
        when(roleRepository.findByCode("USER")).thenReturn(Optional.of(userRole));

        clubService.approveClub(clubId);

        assertThat(user.getRoles()).contains(clubAdminRole);
        assertThat(user.getRoles()).doesNotContain(userRole);
        verify(userRepository).save(user);
    }
}
