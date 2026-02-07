package com.cloud.community.core.aspect;

import com.cloud.community.core.annotation.AuditLog;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.core.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final ExpressionParser parser = new SpelExpressionParser();

    @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        handleLog(joinPoint, auditLog, result);
    }

    private void handleLog(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        try {
            com.cloud.community.core.entity.AuditLog logEntity = new com.cloud.community.core.entity.AuditLog();

            // SpEL Context
            EvaluationContext context = new StandardEvaluationContext();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            if (paramNames != null) {
                for (int i = 0; i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            context.setVariable("result", result);

            // User ID Resolution
            Long userId = null;
            if (StringUtils.hasText(auditLog.userId())) {
                try {
                    userId = parser.parseExpression(auditLog.userId()).getValue(context, Long.class);
                } catch (Exception e) {
                    log.warn("Failed to parse userId SpEL: {}", auditLog.userId());
                }
            }
            
            if (userId == null) {
                userId = getUserIdFromContext();
            }
            
            if (userId != null) {
                logEntity.setUserId(userId);
            }

            // Action
            logEntity.setAction(auditLog.action());
            logEntity.setResourceType(auditLog.resourceType());

            // Resource ID
            if (StringUtils.hasText(auditLog.resourceId())) {
                try {
                    Object val = parser.parseExpression(auditLog.resourceId()).getValue(context);
                    logEntity.setResourceId(val != null ? val.toString() : null);
                } catch (Exception e) {
                     log.warn("Failed to parse resourceId SpEL: {}", auditLog.resourceId());
                }
            }

            // Detail
            if (StringUtils.hasText(auditLog.detail())) {
                try {
                     Object val = parser.parseExpression(auditLog.detail()).getValue(context);
                     logEntity.setDetail(val != null ? val.toString() : null);
                } catch (Exception e) {
                     log.warn("Failed to parse detail SpEL: {}", auditLog.detail());
                }
            }

            // IP
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                logEntity.setIp(getIpAddress(attributes.getRequest()));
            }

            auditLogService.recordLog(logEntity);

        } catch (Exception e) {
            log.error("Error recording audit log", e);
        }
    }

    private Long getUserIdFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                Optional<User> user = userRepository.findByUsername(username);
                return user.map(User::getId).orElse(null);
            } else if (principal instanceof String && !"anonymousUser".equals(principal)) {
                 Optional<User> user = userRepository.findByUsername((String) principal);
                 return user.map(User::getId).orElse(null);
            }
        }
        return null;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
