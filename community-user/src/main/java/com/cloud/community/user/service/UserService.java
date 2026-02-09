package com.cloud.community.user.service;

import com.cloud.community.core.entity.User;

import java.util.Optional;

public interface UserService {
    User register(User user);
    Optional<User> findByUsername(String username);
    User findById(Long id);
    Optional<User> findByEmail(String email);
    void updatePassword(Long userId, String newPassword);
}
