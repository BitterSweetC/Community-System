package com.cloud.community.user.service;

import com.cloud.community.core.entity.Role;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.RoleRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserServiceImpl userService;

    @Test
    void register_encodesPasswordAndAssignsDefaultRole() {
        User user = new User();
        user.setPassword("password123");
        user.setRoles(new HashSet<>());

        Role userRole = new Role();
        when(roleRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.register(user);

        assertThat(result.getPassword()).isEqualTo("encoded_password");
        assertThat(result.getRoles()).contains(userRole);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(user);
    }

    @Test
    void register_throwsWhenDefaultRoleMissing() {
        User user = new User();
        user.setPassword("password123");
        user.setRoles(new HashSet<>());

        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(roleRepository.findByCode("USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Default role USER not found");
    }

    @Test
    void updatePassword_encodesAndSaves() {
        User user = new User();
        user.setPassword("old_password");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass1")).thenReturn("encoded_new");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updatePassword(1L, "newPass1");

        assertThat(user.getPassword()).isEqualTo("encoded_new");
        verify(userRepository).save(user);
    }

    @Test
    void updatePassword_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePassword(99L, "newPass1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}
