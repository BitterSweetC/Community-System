package com.cloud.community.user.service;

import com.cloud.community.core.entity.User;

import java.util.Optional;

public interface UserService {
    User register(User user);
    Optional<User> findByUsername(String username);
    User findById(Long id);
    // Login logic will be handled by Spring Security + JWT, but we might need helper methods here
}
