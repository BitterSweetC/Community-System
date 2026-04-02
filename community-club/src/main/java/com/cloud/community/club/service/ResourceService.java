package com.cloud.community.club.service;

import com.cloud.community.core.entity.Resource;
import com.cloud.community.core.entity.ResourceApplication;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ResourceService {
    // Application Management
    ResourceApplication applyResource(ResourceApplication application);
    void approveResource(Long applicationId, Long approverId);
    void rejectResource(Long applicationId, Long approverId);
    List<ResourceApplication> getClubResources(Long clubId);
    Page<ResourceApplication> getClubResources(Long clubId, int page, int size);
    List<ResourceApplication> getPendingResources();
    Page<ResourceApplication> getPendingResources(int page, int size);
    List<ResourceApplication> getBindableVenueApplications(Long clubId, Long activityId);

    // Resource Management
    List<Resource> getAllResources();
    Page<Resource> getAllResources(int page, int size);
    List<Resource> getAvailableResources();
    Resource createResource(Resource resource);
    Resource updateResource(Resource resource);
    void deleteResource(Long id);
}
