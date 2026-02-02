package com.cloud.community.user.controller;

import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.entity.ActivitySignup;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.ActivityRepository;
import com.cloud.community.core.repository.ActivitySignupRepository;
import com.cloud.community.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ActivitySignupRepository activitySignupRepository;
    private final ActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        }
        throw new RuntimeException("Invalid authentication principal");
    }

    @GetMapping("/me")
    public Result<User> getCurrentUserProfile() {
        return Result.success(getCurrentUser());
    }

    @PutMapping("/me")
    public Result<User> updateProfile(@RequestBody User userRequest) {
        User currentUser = getCurrentUser();
        
        // Update allowed fields
        if (userRequest.getRealName() != null) currentUser.setRealName(userRequest.getRealName());
        if (userRequest.getAvatarUrl() != null) currentUser.setAvatarUrl(userRequest.getAvatarUrl());
        if (userRequest.getMobile() != null) currentUser.setMobile(userRequest.getMobile());
        if (userRequest.getEmail() != null) currentUser.setEmail(userRequest.getEmail());
        if (userRequest.getInterests() != null) currentUser.setInterests(userRequest.getInterests());
        
        userRepository.save(currentUser);
        return Result.success(currentUser);
    }

    @PostMapping("/me/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> passwordMap) {
        User currentUser = getCurrentUser();
        String oldPassword = passwordMap.get("oldPassword");
        String newPassword = passwordMap.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            throw new RuntimeException("Old and new passwords are required");
        }

        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            throw new RuntimeException("Invalid old password");
        }

        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
        return Result.success();
    }

    @GetMapping("/me/activities")
    public Result<List<Activity>> getMyActivities() {
        User currentUser = getCurrentUser();
        List<ActivitySignup> signups = activitySignupRepository.findByUserId(currentUser.getId());
        List<Long> activityIds = signups.stream()
                .map(signup -> signup.getActivity().getId())
                .collect(Collectors.toList());
        
        List<Activity> activities = activityRepository.findAllById(activityIds);
        return Result.success(activities);
    }
}
