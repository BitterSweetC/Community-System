package com.cloud.community.core.service.impl;

import com.cloud.community.core.common.PageResult;
import com.cloud.community.core.entity.AuditLog;
import com.cloud.community.core.repository.AuditLogRepository;
import com.cloud.community.core.service.AuditLogService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Override
    @Transactional
    public void recordLog(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
            log.debug("Audit log recorded: {}", auditLog);
        } catch (Exception e) {
            log.error("Failed to record audit log", e);
        }
    }

    @Override
    public PageResult<AuditLog> searchLogs(Long userId, String action, String resourceType, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            
            if (StringUtils.hasText(action)) {
                predicates.add(cb.like(root.get("action"), "%" + action + "%"));
            }
            
            if (StringUtils.hasText(resourceType)) {
                predicates.add(cb.equal(root.get("resourceType"), resourceType));
            }
            
            // Default sort by created_at desc if not specified in pageable
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                 query.orderBy(cb.desc(root.get("createdAt")));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
        return PageResult.of(page);
    }
}
