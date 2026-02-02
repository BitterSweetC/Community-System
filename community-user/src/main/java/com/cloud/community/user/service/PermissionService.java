package com.cloud.community.user.service;

public interface PermissionService {
    /**
     * Check if the user has permission to manage the specified club.
     * Allows if:
     * 1. User is a global ADMIN.
     * 2. User is a member of the club with PRESIDENT or MANAGER role.
     * Throws RuntimeException if not authorized.
     */
    void checkClubAdmin(Long userId, Long clubId);

    /**
     * Check if the user is a global ADMIN.
     */
    void checkSystemAdmin(Long userId);

    /**
     * Check if the club is active (not dissolved or pending).
     * Throws RuntimeException if club is not active.
     */
    void checkClubActive(Long clubId);
}
