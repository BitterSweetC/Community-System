package com.cloud.community.club.service.impl;

import com.cloud.community.club.service.ResourceService;
import com.cloud.community.core.constant.RabbitConstants;
import com.cloud.community.core.entity.Resource;
import com.cloud.community.core.entity.ResourceApplication;
import com.cloud.community.core.exception.BusinessException;
import com.cloud.community.core.metrics.BusinessMetricsService;
import com.cloud.community.core.model.dto.NotificationMessageDTO;
import com.cloud.community.core.repository.ResourceApplicationRepository;
import com.cloud.community.core.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceApplicationRepository resourceRepository;
    private final ResourceRepository resourceDefinitionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final BusinessMetricsService metricsService;

    @Override
    @Transactional
    public ResourceApplication applyResource(ResourceApplication application) {
        if (application.getResourceId() == null) {
            throw new BusinessException(40041, "资源编号不能为空");
        }

        // Lock resource row to serialize availability checks for the same resource.
        Resource resource = resourceDefinitionRepository.findByIdForUpdate(application.getResourceId())
                .orElseThrow(() -> new RuntimeException("未发现资源"));
        application.setResource(resource);

        int requested = normalizeRequestedQuantity(application.getQuantity());
        application.setQuantity(requested);

        validateAvailability(resource, application.getStartTime(), application.getEndTime(), requested);

        application.setStatus("PENDING");
        return resourceRepository.save(application);
    }

    @Override
    @Transactional
    public void approveResource(Long applicationId, Long approverId) {
        // Lock application row to prevent double approval/rejection.
        ResourceApplication application = resourceRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new RuntimeException("未发现资源申请"));

        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException(40941, "该资源申请已处理，请勿重复审批");
        }

        // Lock resource row and re-check availability during approval.
        Resource resource = resourceDefinitionRepository.findByIdForUpdate(application.getResourceId())
                .orElseThrow(() -> new RuntimeException("未发现资源"));
        int requested = normalizeRequestedQuantity(application.getQuantity());
        validateAvailability(resource, application.getStartTime(), application.getEndTime(), requested);
        application.setQuantity(requested);

        application.setStatus("APPROVED");
        application.setApproverId(approverId);
        resourceRepository.save(application);
        metricsService.recordApprovalHandled("resource", "approved", application.getCreatedAt());

        String resourceName = resourceDefinitionRepository.findById(application.getResourceId())
                .map(Resource::getName)
                .orElse("未知资源");
        sendResourceNotification(
                application.getApplicantId(),
                "资源申请已通过",
                "您申请的资源【" + resourceName + "】已审批通过。");
        pushPendingCount();
    }

    @Override
    @Transactional
    public void rejectResource(Long applicationId, Long approverId) {
        ResourceApplication application = resourceRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new RuntimeException("未发现资源申请"));

        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException(40941, "该资源申请已处理，请勿重复审批");
        }

        application.setStatus("REJECTED");
        application.setApproverId(approverId);
        resourceRepository.save(application);
        metricsService.recordApprovalHandled("resource", "rejected", application.getCreatedAt());

        String resourceName = resourceDefinitionRepository.findById(application.getResourceId())
                .map(Resource::getName)
                .orElse("未知资源");
        sendResourceNotification(
                application.getApplicantId(),
                "资源申请未通过",
                "您申请的资源【" + resourceName + "】未通过审批，请联系管理员了解详情。");
        pushPendingCount();
    }

    private int normalizeRequestedQuantity(Integer quantity) {
        int requested = quantity == null ? 1 : quantity;
        if (requested <= 0) {
            throw new BusinessException(40042, "申请数量必须大于 0");
        }
        return requested;
    }

    private void validateAvailability(Resource resource, java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime, int requested) {
        if ("MATERIAL".equals(resource.getType())) {
            if (resource.getTotalQuantity() != null && requested > resource.getTotalQuantity()) {
                throw new BusinessException(40942, "申请数量超过资源总库存");
            }

            int alreadyApproved = resourceRepository.sumApprovedQuantityInPeriod(
                    resource.getId(), startTime, endTime);
            if (resource.getTotalQuantity() != null && alreadyApproved + requested > resource.getTotalQuantity()) {
                int remaining = resource.getTotalQuantity() - alreadyApproved;
                throw new BusinessException(40943, "该时间段库存不足，当前剩余可申请数量：" + remaining);
            }
            return;
        }

        List<ResourceApplication> conflicts = resourceRepository.findConflictingApplications(
                resource.getId(), startTime, endTime);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(40944, "该时间段内场地已被预约");
        }
    }

    private void pushPendingCount() {
        long pending = resourceRepository.countByStatus("PENDING");
        messagingTemplate.convertAndSend("/topic/dashboard/stats",
                java.util.Map.of("pendingResources", pending));
    }

    private void sendResourceNotification(Long userId, String title, String content) {
        try {
            NotificationMessageDTO message = new NotificationMessageDTO();
            message.setUserId(userId);
            message.setTitle(title);
            message.setContent(content);
            message.setType("RESOURCE");
            rabbitTemplate.convertAndSend(
                    RabbitConstants.NOTIFICATION_EXCHANGE,
                    RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY,
                    message);
        } catch (Exception e) {
            log.error("Failed to send resource notification to user {}", userId, e);
        }
    }

    @Override
    public List<ResourceApplication> getClubResources(Long clubId) {
        return resourceRepository.findByClubId(clubId);
    }

    @Override
    public Page<ResourceApplication> getClubResources(Long clubId, int page, int size) {
        return resourceRepository.findByClubIdOrderByCreatedAtDesc(clubId, PageRequest.of(page, size));
    }

    @Override
    public List<ResourceApplication> getPendingResources() {
        return resourceRepository.findByStatus("PENDING");
    }

    @Override
    public Page<ResourceApplication> getPendingResources(int page, int size) {
        return resourceRepository.findByStatusOrderByCreatedAtDesc("PENDING", PageRequest.of(page, size));
    }

    @Override
    public List<ResourceApplication> getBindableVenueApplications(Long clubId, Long activityId) {
        return resourceRepository.findBindableVenueApplications(clubId, LocalDateTime.now(), activityId);
    }

    @Override
    public List<Resource> getAllResources() {
        return resourceDefinitionRepository.findAll();
    }

    @Override
    public Page<Resource> getAllResources(int page, int size) {
        return resourceDefinitionRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Override
    public List<Resource> getAvailableResources() {
        return resourceDefinitionRepository.findByStatus("AVAILABLE");
    }

    @Override
    @Transactional
    public Resource createResource(Resource resource) {
        return resourceDefinitionRepository.save(resource);
    }

    @Override
    @Transactional
    public Resource updateResource(Resource resource) {
        if (resource.getId() == null || !resourceDefinitionRepository.existsById(resource.getId())) {
            throw new RuntimeException("未发现资源");
        }
        return resourceDefinitionRepository.save(resource);
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        resourceDefinitionRepository.deleteById(id);
    }
}
