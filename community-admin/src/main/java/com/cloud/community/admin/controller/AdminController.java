package com.cloud.community.admin.controller;

import com.cloud.community.core.annotation.AuditLog;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.club.service.ClubService;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.cloud.community.core.model.vo.ClubVO;
import com.cloud.community.core.entity.Club;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ClubService clubService;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final UserService userService;

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null ||
            !SecurityContextHolder.getContext().getAuthentication().isAuthenticated() ||
            "anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
            throw new RuntimeException("User not authenticated");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @GetMapping("/clubs/pending")
    public Result<List<ClubVO>> getPendingClubs() {
        User user = getCurrentUser();
        permissionService.checkSystemAdmin(user.getId());
        List<Club> clubs = clubService.getPendingClubs();
        return Result.success(clubs.stream().map(ClubVO::from).collect(Collectors.toList()));
    }

    @GetMapping("/clubs/dissolving")
    public Result<List<ClubVO>> getDissolvingClubs() {
        User user = getCurrentUser();
        permissionService.checkSystemAdmin(user.getId());
        List<Club> clubs = clubService.getDissolvingClubs();
        return Result.success(clubs.stream().map(ClubVO::from).collect(Collectors.toList()));
    }

    @AuditLog(action = "APPROVE_CLUB", resourceType = "CLUB", resourceId = "#id")
    @PostMapping("/clubs/{id}/approve")
    public Result<Void> approveClub(@PathVariable Long id) {
        User user = getCurrentUser();
        permissionService.checkSystemAdmin(user.getId());
        clubService.approveClub(id);
        return Result.success();
    }

    @AuditLog(action = "APPROVE_DISSOLUTION", resourceType = "CLUB", resourceId = "#id")
    @PostMapping("/clubs/{id}/approve-dissolution")
    public Result<Void> approveDissolution(@PathVariable Long id) {
        User user = getCurrentUser();
        permissionService.checkSystemAdmin(user.getId());
        clubService.approveDissolution(id, user.getId());
        return Result.success();
    }

    @AuditLog(action = "REJECT_DISSOLUTION", resourceType = "CLUB", resourceId = "#id")
    @PostMapping("/clubs/{id}/reject-dissolution")
    public Result<Void> rejectDissolution(@PathVariable Long id) {
        User user = getCurrentUser();
        permissionService.checkSystemAdmin(user.getId());
        clubService.rejectDissolution(id, user.getId());
        return Result.success();
    }

    @AuditLog(action = "DELETE_CLUB", resourceType = "CLUB", resourceId = "#id")
    @DeleteMapping("/clubs/{id}")
    public Result<Void> deleteClub(@PathVariable Long id) {
        User user = getCurrentUser();
        permissionService.checkSystemAdmin(user.getId());
        clubService.deleteClub(id, user.getId());
        return Result.success();
    }

    @AuditLog(action = "UPDATE_USER_STATUS", resourceType = "USER", resourceId = "#id", detail = "#status")
    @PostMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam String status) {
        User user = getCurrentUser();
        permissionService.checkSystemAdmin(user.getId());
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        targetUser.setStatus(status);
        userRepository.save(targetUser);
        return Result.success();
    }

    @AuditLog(action = "CLEANUP_ROLES", resourceType = "SYSTEM")
    @PostMapping("/roles/cleanup")
    public Result<Integer> cleanupOrphanedRoles() {
        User user = getCurrentUser();
        permissionService.checkSystemAdmin(user.getId());
        int count = clubService.cleanupOrphanedClubAdminRoles();
        return Result.success(count);
    }
}
