package com.cloud.community.recruit.service.impl;

import com.cloud.community.core.entity.Club;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.core.entity.RecruitApplication;
import com.cloud.community.core.entity.RecruitBatch;
import com.cloud.community.core.entity.RecruitFormField;
import com.cloud.community.core.repository.RecruitApplicationRepository;
import com.cloud.community.core.repository.RecruitBatchRepository;
import com.cloud.community.core.repository.RecruitFormFieldRepository;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.club.service.ClubService;
import com.cloud.community.recruit.service.RecruitService;
import com.cloud.community.core.model.dto.NotificationMessageDTO;
import com.cloud.community.core.constant.RabbitConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecruitServiceImpl implements RecruitService {

    private final RecruitBatchRepository batchRepository;
    private final RecruitFormFieldRepository formFieldRepository;
    private final RecruitApplicationRepository applicationRepository;
    private final MemberRepository memberRepository;
    private final ClubService clubService;
    private final PermissionService permissionService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public RecruitBatch createBatch(RecruitBatch batch, Long operatorId) {
        permissionService.checkClubAdmin(operatorId, batch.getClub().getId());
        permissionService.checkClubActive(batch.getClub().getId());
        return batchRepository.save(batch);
    }

    @Override
    public List<RecruitBatch> getBatchesByClub(Long clubId) {
        return batchRepository.findByClubId(clubId);
    }

    @Override
    public RecruitBatch getBatchById(Long batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Recruit Batch not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Club> getRecruitingClubs() {
        return batchRepository.findClubsWithActiveRecruitment(java.time.LocalDateTime.now());
    }

    @Override
    @Transactional
    public void addFormField(RecruitFormField field, Long operatorId) {
        RecruitBatch batch = batchRepository.findById(field.getBatch().getId())
                .orElseThrow(() -> new RuntimeException("Recruit Batch not found"));
        permissionService.checkClubAdmin(operatorId, batch.getClub().getId());
        permissionService.checkClubActive(batch.getClub().getId());
        formFieldRepository.save(field);
    }

    @Override
    public List<RecruitFormField> getFormFields(Long batchId) {
        return formFieldRepository.findByBatchIdOrderBySortOrderAsc(batchId);
    }

    @Override
    @Transactional
    public void submitApplication(RecruitApplication application) {
        // 1. Fetch full batch to check status/time
        RecruitBatch batch = batchRepository.findById(application.getBatch().getId())
                .orElseThrow(() -> new RuntimeException("Recruit Batch not found"));
        
        // 2. Check if batch is active
        permissionService.checkClubActive(batch.getClub().getId());
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isBefore(batch.getStartTime())) {
            throw new RuntimeException("Recruitment has not started yet");
        }
        if (now.isAfter(batch.getEndTime())) {
            throw new RuntimeException("Recruitment has ended");
        }
        
        // 3. Check if already applied
        if (applicationRepository.findByBatchIdAndUserId(
                batch.getId(), 
                application.getUser().getId()).isPresent()) {
            throw new RuntimeException("Already applied to this batch");
        }

        // 4. Check if already a member of the club
        if (memberRepository.findByClubIdAndUserId(batch.getClub().getId(), application.getUser().getId()).isPresent()) {
            throw new RuntimeException("You are already a member of this club");
        }
        
        // 5. Set full batch object to application
        application.setBatch(batch);
        
        applicationRepository.save(application);
    }

    @Override
    public List<RecruitApplication> getApplicationsByBatch(Long batchId, Long operatorId) {
        RecruitBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Recruit Batch not found"));
        permissionService.checkClubAdmin(operatorId, batch.getClub().getId());
        return applicationRepository.findByBatchId(batchId);
    }

    @Override
    public List<RecruitApplication> getMyApplications(Long userId) {
        return applicationRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void reviewApplicationFirst(Long applicationId, boolean pass, String comment, Long operatorId) {
        RecruitApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        
        permissionService.checkClubAdmin(operatorId, app.getBatch().getClub().getId());
        permissionService.checkClubActive(app.getBatch().getClub().getId());
        
        app.setFirstReviewStatus(pass ? "PASSED" : "REJECTED");
        app.setFirstReviewComment(comment);
        applicationRepository.save(app);

        // Send notification
        try {
            NotificationMessageDTO message = new NotificationMessageDTO();
            message.setUserId(app.getUser().getId());
            message.setTitle("Recruitment First Review Result");
            message.setContent(pass ? 
                "Congratulations! You have passed the first review for " + app.getBatch().getClub().getName() :
                "Sorry, you did not pass the first review for " + app.getBatch().getClub().getName() + ". Reason: " + comment);
            message.setType("SYSTEM");
            rabbitTemplate.convertAndSend(RabbitConstants.NOTIFICATION_EXCHANGE, RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY, message);
        } catch (Exception e) {
            // Log error but don't fail transaction
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public void reviewApplicationFinal(Long applicationId, boolean pass, String comment, Long operatorId) {
        RecruitApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        
        permissionService.checkClubAdmin(operatorId, app.getBatch().getClub().getId());
        permissionService.checkClubActive(app.getBatch().getClub().getId());
        
        app.setFinalReviewStatus(pass ? "PASSED" : "REJECTED");
        app.setFinalReviewComment(comment);
        
        if (pass) {
            // Auto-add to club member
            // clubService.addMember currently does not check permission if we assume this internal call is safe
            // However, addMember signature might stay same, or we might need to bypass permission check for internal system action
            // But clubService.addMember calls repository directly in current impl.
            // If we add permission check to clubService.addMember later, we need to be careful.
            // Current plan: I am adding permission check in ClubController, not ClubService.
            clubService.addMember(app.getBatch().getClub().getId(), app.getUser().getId(), "MEMBER");
        }
        
        applicationRepository.save(app);

        // Send notification
        try {
            NotificationMessageDTO message = new NotificationMessageDTO();
            message.setUserId(app.getUser().getId());
            message.setTitle("Recruitment Final Review Result");
            message.setContent(pass ? 
                "Congratulations! You have been accepted into " + app.getBatch().getClub().getName() :
                "Sorry, you did not pass the final review for " + app.getBatch().getClub().getName() + ". Reason: " + comment);
            message.setType("SYSTEM");
            rabbitTemplate.convertAndSend(RabbitConstants.NOTIFICATION_EXCHANGE, RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY, message);
        } catch (Exception e) {
            // Log error but don't fail transaction
            e.printStackTrace();
        }
    }
}
