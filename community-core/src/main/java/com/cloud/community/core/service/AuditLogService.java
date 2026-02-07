package com.cloud.community.core.service;

import com.cloud.community.core.common.PageResult;
import com.cloud.community.core.entity.AuditLog;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    
    /**
     * Record an audit log
     * @param auditLog log entity
     */
    void recordLog(AuditLog auditLog);

    /**
     * Search audit logs
     * @param userId filter by user id
     * @param action filter by action
     * @param resourceType filter by resource type
     * @param pageable pagination info
     * @return page result
     */
    PageResult<AuditLog> searchLogs(Long userId, String action, String resourceType, Pageable pageable);
}
