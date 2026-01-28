package com.cloud.community.club.service.impl;

import com.cloud.community.core.entity.Role;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.RoleRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.club.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(User user) {
        // Password encoding is handled by PasswordEncoder bean, which is now NoOp
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Assign default role USER
        Role userRole = roleRepository.findByCode("USER")
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));
        user.getRoles().add(userRole);
        
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
