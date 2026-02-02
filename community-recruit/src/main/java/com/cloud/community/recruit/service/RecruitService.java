package com.cloud.community.recruit.service;

import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.RecruitApplication;
import com.cloud.community.core.entity.RecruitBatch;
import com.cloud.community.core.entity.RecruitFormField;

import java.util.List;

public interface RecruitService {
    RecruitBatch createBatch(RecruitBatch batch, Long operatorId);
    List<RecruitBatch> getBatchesByClub(Long clubId);
    RecruitBatch getBatchById(Long batchId);
    
    List<Club> getRecruitingClubs();

    void addFormField(RecruitFormField field, Long operatorId);
    List<RecruitFormField> getFormFields(Long batchId);
    
    void submitApplication(RecruitApplication application);
    List<RecruitApplication> getApplicationsByBatch(Long batchId, Long operatorId);
    List<RecruitApplication> getMyApplications(Long userId);
    
    void reviewApplicationFirst(Long applicationId, boolean pass, String comment, Long operatorId);
    void reviewApplicationFinal(Long applicationId, boolean pass, String comment, Long operatorId);
}
