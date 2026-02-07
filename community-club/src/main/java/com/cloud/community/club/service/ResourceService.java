package com.cloud.community.club.service;

import com.cloud.community.core.entity.Resource;
import com.cloud.community.core.entity.ResourceApplication;
import java.util.List;

public interface ResourceService {
    // Application Management
    ResourceApplication applyResource(ResourceApplication application);
    void approveResource(Long applicationId, Long approverId);
    void rejectResource(Long applicationId, Long approverId);
    List<ResourceApplication> getClubResources(Long clubId);
    List<ResourceApplication> getPendingResources();

    // Resource Management
    List<Resource> getAllResources();
    List<Resource> getAvailableResources();
    Resource createResource(Resource resource);
    Resource updateResource(Resource resource);
    void deleteResource(Long id);
}
