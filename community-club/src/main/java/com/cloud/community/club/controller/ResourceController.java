package com.cloud.community.club.controller;

import com.cloud.community.club.service.ResourceService;
import com.cloud.community.core.annotation.AuditLog;
import com.cloud.community.core.common.PageResult;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.Resource;
import com.cloud.community.core.entity.ResourceApplication;
import com.cloud.community.core.repository.ClubRepository;
import com.cloud.community.core.entity.User;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final ClubRepository clubRepository;
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
    public Result<PageResult<Resource>> getAllResources(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        User user = getCurrentUser();
        checkAdmin(user);
        return Result.success(PageResult.of(resourceService.getAllResources(page, size)));
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
        if (application.getClubId() == null) {
            throw new RuntimeException("Club ID is required");
        }
        permissionService.checkClubAdmin(user.getId(), application.getClubId());
        application.setApplicantId(user.getId());
        return Result.success(resourceService.applyResource(application));
    }

    @GetMapping("/clubs/{clubId}/applications")
    public Result<PageResult<ResourceApplication>> getClubResources(@PathVariable Long clubId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        User user = getCurrentUser();
        permissionService.checkClubAdmin(user.getId(), clubId);
        PageResult<ResourceApplication> result = PageResult.of(resourceService.getClubResources(clubId, page, size));
        enrichResourceApplicationsDisplayFields(result.getList());
        return Result.success(result);
    }

    @GetMapping("/clubs/{clubId}/activity-bindable-applications")
    public Result<List<ResourceApplication>> getActivityBindableApplications(@PathVariable Long clubId,
                                                                             @RequestParam(required = false) Long activityId) {
        User user = getCurrentUser();
        permissionService.checkClubAdmin(user.getId(), clubId);
        permissionService.checkClubActive(clubId);
        return Result.success(resourceService.getBindableVenueApplications(clubId, activityId));
    }

    @GetMapping("/applications/pending")
    public Result<PageResult<ResourceApplication>> getPendingResources(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size) {
        User user = getCurrentUser();
        checkAdmin(user);
        PageResult<ResourceApplication> result = PageResult.of(resourceService.getPendingResources(page, size));
        enrichResourceApplicationsDisplayFields(result.getList());
        return Result.success(result);
    }

    @AuditLog(action = "APPROVE_RESOURCE", resourceType = "RESOURCE_APPLICATION", resourceId = "#id")
    @PostMapping("/applications/{id}/approve")
    public Result<Void> approveResource(@PathVariable Long id) {
        User user = getCurrentUser();
        checkAdmin(user);
        resourceService.approveResource(id, user.getId());
        return Result.success(null);
    }

    @AuditLog(action = "REJECT_RESOURCE", resourceType = "RESOURCE_APPLICATION", resourceId = "#id")
    @PostMapping("/applications/{id}/reject")
    public Result<Void> rejectResource(@PathVariable Long id) {
        User user = getCurrentUser();
        checkAdmin(user);
        resourceService.rejectResource(id, user.getId());
        return Result.success(null);
    }

    private void enrichResourceApplicationsDisplayFields(List<ResourceApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return;
        }

        List<Long> clubIds = applications.stream()
                .map(ResourceApplication::getClubId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (clubIds.isEmpty()) {
            return;
        }

        Map<Long, Club> clubMap = loadClubMap(clubIds);
        for (ResourceApplication application : applications) {
            Club club = clubMap.get(application.getClubId());
            if (club != null) {
                application.setClubName(club.getName());
            }
        }
    }

    private Map<Long, Club> loadClubMap(List<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return StreamSupport.stream(clubRepository.findAllById(clubIds).spliterator(), false)
                .collect(Collectors.toMap(Club::getId, Function.identity()));
    }
}
