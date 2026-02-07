package com.cloud.community.club.service.impl;

import com.cloud.community.club.service.ResourceService;
import com.cloud.community.core.entity.Resource;
import com.cloud.community.core.entity.ResourceApplication;
import com.cloud.community.core.repository.ResourceApplicationRepository;
import com.cloud.community.core.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceApplicationRepository resourceRepository;
    private final ResourceRepository resourceDefinitionRepository;

    @Override
    @Transactional
    public ResourceApplication applyResource(ResourceApplication application) {
        if (application.getResource() == null || application.getResource().getId() == null) {
            throw new RuntimeException("Resource is required");
        }
        
        Resource resource = resourceDefinitionRepository.findById(application.getResource().getId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        application.setResource(resource);

        // Check for conflicts
        List<ResourceApplication> conflicts = resourceRepository.findConflictingApplications(
                resource.getId(), application.getStartTime(), application.getEndTime());
        
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Resource is already booked for this time period");
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
