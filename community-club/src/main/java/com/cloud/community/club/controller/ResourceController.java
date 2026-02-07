package com.cloud.community.club.controller;

import com.cloud.community.club.service.ResourceService;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.Resource;
import com.cloud.community.core.entity.ResourceApplication;
import com.cloud.community.core.entity.User;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final UserService userService;
    private final PermissionService permissionService;

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null ||
            !SecurityContextHolder.getContext().getAuthentication().isAuthenticated() ||
            "anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
            throw new RuntimeException("User not authenticated");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Resource Definitions
    @GetMapping("/list")
    public Result<List<Resource>> getAvailableResources() {
        return Result.success(resourceService.getAvailableResources());
    }

    @GetMapping("/admin/list")
    public Result<List<Resource>> getAllResources() {
        User user = getCurrentUser();
        checkAdmin(user);
        return Result.success(resourceService.getAllResources());
    }

    @PostMapping("/admin")
    public Result<Resource> createResource(@RequestBody Resource resource) {
        User user = getCurrentUser();
        checkAdmin(user);
        return Result.success(resourceService.createResource(resource));
    }

    @PutMapping("/admin")
    public Result<Resource> updateResource(@RequestBody Resource resource) {
        User user = getCurrentUser();
        checkAdmin(user);
        return Result.success(resourceService.updateResource(resource));
    }

    @DeleteMapping("/admin/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        User user = getCurrentUser();
        checkAdmin(user);
        resourceService.deleteResource(id);
        return Result.success(null);
    }

    private void checkAdmin(User user) {
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()));
        if (!isAdmin) {
            throw new RuntimeException("Permission denied");
        }
    }

    // Resource Applications
    @PostMapping("/applications")
    public Result<ResourceApplication> applyResource(@RequestBody ResourceApplication application) {
        User user = getCurrentUser();
        if (application.getClub() == null || application.getClub().getId() == null) {
            throw new RuntimeException("Club ID is required");
        }
        permissionService.checkClubAdmin(user.getId(), application.getClub().getId());
        application.setApplicantId(user.getId());
        return Result.success(resourceService.applyResource(application));
    }

    @GetMapping("/clubs/{clubId}/applications")
    public Result<List<ResourceApplication>> getClubResources(@PathVariable Long clubId) {
        User user = getCurrentUser();
        permissionService.checkClubAdmin(user.getId(), clubId);
        return Result.success(resourceService.getClubResources(clubId));
    }

    @GetMapping("/applications/pending")
    public Result<List<ResourceApplication>> getPendingResources() {
        User user = getCurrentUser();
        checkAdmin(user);
        return Result.success(resourceService.getPendingResources());
    }

    @PostMapping("/applications/{id}/approve")
    public Result<Void> approveResource(@PathVariable Long id) {
        User user = getCurrentUser();
        resourceService.approveResource(id, user.getId());
        return Result.success(null);
    }

    @PostMapping("/applications/{id}/reject")
    public Result<Void> rejectResource(@PathVariable Long id) {
        User user = getCurrentUser();
        resourceService.rejectResource(id, user.getId());
        return Result.success(null);
    }
}
