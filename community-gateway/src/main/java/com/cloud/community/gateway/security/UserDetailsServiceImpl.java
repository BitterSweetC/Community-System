package com.cloud.community.gateway.security;

import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.Member;
import com.cloud.community.core.entity.Role;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.core.repository.RoleRepository;
import com.cloud.community.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        user = normalizeClubAdminRole(user);

        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                "ACTIVE".equals(user.getStatus()),
                true,
                true,
                true,
                authorities
        );
    }

    private User normalizeClubAdminRole(User user) {
        Role clubAdminRole = roleRepository.findByCode("CLUB_ADMIN").orElse(null);
        Role userRole = roleRepository.findByCode("USER").orElse(null);

        if (clubAdminRole == null) {
            return user;
        }

        List<Member> memberships = memberRepository.findByUserId(user.getId());
        boolean shouldHaveClubAdmin = memberships.stream()
                .anyMatch(member -> "ACTIVE".equals(member.getStatus())
                        && member.getClub() != null
                        && Club.STATUS_ACTIVE.equals(member.getClub().getStatus())
                        && ("PRESIDENT".equals(member.getRoleCode()) || "MANAGER".equals(member.getRoleCode())));

        boolean hasClubAdmin = user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> "CLUB_ADMIN".equals(role.getCode()));
        boolean hasUserRole = user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> "USER".equals(role.getCode()));
        boolean shouldEnsureUserRole = userRole != null && !hasUserRole;

        if (shouldHaveClubAdmin == hasClubAdmin && !shouldEnsureUserRole) {
            return user;
        }

        User lockedUser = userRepository.findByIdForUpdate(user.getId()).orElse(user);
        if (lockedUser.getRoles() == null) {
            lockedUser.setRoles(new HashSet<>());
        }

        lockedUser.getRoles().removeIf(role -> "CLUB_ADMIN".equals(role.getCode()) && !shouldHaveClubAdmin);
        if (shouldHaveClubAdmin && lockedUser.getRoles().stream().noneMatch(role -> "CLUB_ADMIN".equals(role.getCode()))) {
            lockedUser.getRoles().add(clubAdminRole);
        }
        if (userRole != null && lockedUser.getRoles().stream().noneMatch(role -> "USER".equals(role.getCode()))) {
            lockedUser.getRoles().add(userRole);
        }

        return userRepository.save(lockedUser);
    }
}
