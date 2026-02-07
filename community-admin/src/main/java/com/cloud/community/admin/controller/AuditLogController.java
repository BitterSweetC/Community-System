package com.cloud.community.admin.controller;

import com.cloud.community.core.common.PageResult;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.AuditLog;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.model.vo.AuditLogVO;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.core.service.AuditLogService;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final PermissionService permissionService;
    private final UserService userService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null ||
            !SecurityContextHolder.getContext().getAuthentication().isAuthenticated() ||
            "anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
            throw new RuntimeException("User not authenticated");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @GetMapping
    public Result<PageResult<AuditLogVO>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        User currentUser = getCurrentUser();
        permissionService.checkSystemAdmin(currentUser.getId());
        
        PageResult<AuditLog> result = auditLogService.searchLogs(userId, action, resourceType, PageRequest.of(page - 1, size));
        
        Set<Long> userIds = result.getList().stream()
                .map(AuditLog::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
                
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
                
        PageResult<AuditLogVO> voResult = result.map(log -> {
            User u = log.getUserId() != null ? userMap.get(log.getUserId()) : null;
            return AuditLogVO.from(log, 
                u != null ? u.getUsername() : "Unknown", 
                u != null ? u.getRealName() : "Unknown");
        });
        
        return Result.success(voResult);
    }
}
