package com.cloud.community.club.service.impl;

import com.cloud.community.club.service.ResourceService;
import com.cloud.community.core.constant.RabbitConstants;
import com.cloud.community.core.entity.Resource;
import com.cloud.community.core.entity.ResourceApplication;
import com.cloud.community.core.model.dto.NotificationMessageDTO;
import com.cloud.community.core.repository.ResourceApplicationRepository;
import com.cloud.community.core.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceApplicationRepository resourceRepository;
    private final ResourceRepository resourceDefinitionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public ResourceApplication applyResource(ResourceApplication application) {
        if (application.getResource() == null || application.getResource().getId() == null) {
            throw new RuntimeException("Resource is required");
        }

        Resource resource = resourceDefinitionRepository.findById(application.getResource().getId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        application.setResource(resource);

        int requested = application.getQuantity() == null ? 1 : application.getQuantity();
        if (requested <= 0) {
            throw new RuntimeException("申请数量必须大于 0");
        }

        if ("MATERIAL".equals(resource.getType())) {
            // 物资：校验不超过总库存，并检查时间段内已批准数量
            if (resource.getTotalQuantity() != null && requested > resource.getTotalQuantity()) {
                throw new RuntimeException("申请数量超过资源总库存（共 " + resource.getTotalQuantity() + " 件）");
            }
            int alreadyApproved = resourceRepository.sumApprovedQuantityInPeriod(
                    resource.getId(), application.getStartTime(), application.getEndTime());
            if (resource.getTotalQuantity() != null && alreadyApproved + requested > resource.getTotalQuantity()) {
                int remaining = resource.getTotalQuantity() - alreadyApproved;
                throw new RuntimeException("该时间段内库存不足，当前剩余可申请数量：" + remaining);
            }
        } else {
            // 场地：同一时间段只允许一个已批准预约
            List<ResourceApplication> conflicts = resourceRepository.findConflictingApplications(
                    resource.getId(), application.getStartTime(), application.getEndTime());
            if (!conflicts.isEmpty()) {
                throw new RuntimeException("该时间段内场地已被预约");
            }
        }

        application.setStatus("PENDING");
        return resourceRepository.save(application);
    }

    @Override
    @Transactional
    public void approveResource(Long applicationId, Long approverId) {
        ResourceApplication application = resourceRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!"PENDING".equals(application.getStatus())) {
            throw new RuntimeException("Application is not pending");
        }

        application.setStatus("APPROVED");
        application.setApproverId(approverId);
        resourceRepository.save(application);

        sendResourceNotification(
                application.getApplicantId(),
                "资源申请已通过",
                "您申请的资源「" + application.getResource().getName() + "」已审批通过。");
    }

    @Override
    @Transactional
    public void rejectResource(Long applicationId, Long approverId) {
        ResourceApplication application = resourceRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!"PENDING".equals(application.getStatus())) {
            throw new RuntimeException("Application is not pending");
        }

        application.setStatus("REJECTED");
        application.setApproverId(approverId);
        resourceRepository.save(application);

        sendResourceNotification(
                application.getApplicantId(),
                "资源申请未通过",
                "您申请的资源「" + application.getResource().getName() + "」未获审批通过，请联系管理员了解详情。");
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
    public List<ResourceApplication> getPendingResources() {
        return resourceRepository.findByStatus("PENDING");
    }

    @Override
    public List<Resource> getAllResources() {
        return resourceDefinitionRepository.findAll();
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
            throw new RuntimeException("Resource not found");
        }
        return resourceDefinitionRepository.save(resource);
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        resourceDefinitionRepository.deleteById(id);
    }
}
