package com.cloud.community.recruit.service.impl;

import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.Member;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.core.entity.RecruitApplication;
import com.cloud.community.core.entity.RecruitBatch;
import com.cloud.community.core.entity.RecruitFormField;
import com.cloud.community.core.exception.BusinessException;
import com.cloud.community.core.metrics.BusinessMetricsService;
import com.cloud.community.core.repository.RecruitApplicationRepository;
import com.cloud.community.core.repository.RecruitBatchRepository;
import com.cloud.community.core.repository.RecruitFormFieldRepository;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.recruit.service.RecruitService;
import com.cloud.community.club.service.ClubService;
import com.cloud.community.core.model.dto.NotificationMessageDTO;
import com.cloud.community.core.constant.RabbitConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final RabbitTemplate rabbitTemplate;
    private final ClubService clubService;
    private final BusinessMetricsService metricsService;

    @Override
    @Transactional
    public RecruitBatch createBatch(RecruitBatch batch, Long operatorId) {
        permissionService.checkClubAdmin(operatorId, batch.getClub().getId());
        permissionService.checkClubActive(batch.getClub().getId());
        return batchRepository.save(batch);
    }

    @Override
    public List<RecruitBatch> getBatchesByClub(Long clubId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return batchRepository.findByClubIdOrderByStartTimeDesc(clubId).stream()
                .sorted((left, right) -> compareBatchOrder(left, right, now))
                .toList();
    }

    @Override
    public Page<RecruitBatch> getBatchesByClub(Long clubId, int page, int size) {
        return batchRepository.findByClubIdOrderByCreatedAtDesc(clubId, PageRequest.of(page, size));
    }

    @Override
    public RecruitBatch getBatchById(Long batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("未发现招新批次"));
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
                .orElseThrow(() -> new RuntimeException("未发现招新批次"));
        permissionService.checkClubAdmin(operatorId, batch.getClub().getId());
        permissionService.checkClubActive(batch.getClub().getId());
        formFieldRepository.save(field);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitFormField> getFormFields(Long batchId) {
        RecruitBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("未发现招新批次"));

        List<RecruitFormField> currentFields = formFieldRepository.findByBatchIdOrderBySortOrderAsc(batchId);
        if (!currentFields.isEmpty()) {
            return currentFields;
        }

        Long clubId = batch.getClub().getId();
        for (RecruitBatch candidate : batchRepository.findByClubIdOrderByStartTimeDesc(clubId)) {
            if (candidate.getId().equals(batchId)) {
                continue;
            }
            List<RecruitFormField> candidateFields = formFieldRepository
                    .findByBatchIdOrderBySortOrderAsc(candidate.getId());
            if (!candidateFields.isEmpty()) {
                return candidateFields.stream()
                        .map(field -> copyFieldForBatch(field, batch))
                        .toList();
            }
        }

        return buildDefaultFields(batch);
    }

    @Override
    @Transactional
    public void submitApplication(RecruitApplication application) {
        // Lock User to prevent double submission for same user (lighter than locking
        // batch)
        userRepository.findByIdForUpdate(application.getUser().getId());

        // 1. Fetch full batch to check status/time
        RecruitBatch batch = batchRepository.findById(application.getBatch().getId())
                .orElseThrow(() -> new RuntimeException("未发现招新批次"));

        // 2. Check if batch is active
        permissionService.checkClubActive(batch.getClub().getId());
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isBefore(batch.getStartTime())) {
            throw new RuntimeException("还未开始招新");
        }
        if (now.isAfter(batch.getEndTime())) {
            throw new RuntimeException("招新已结束");
        }

        // 3. Check if already applied
        if (applicationRepository.findByBatchIdAndUserId(
                batch.getId(),
                application.getUser().getId()).isPresent()) {
            throw new BusinessException(40911, "您已提交过该招新批次申请，请勿重复提交");
        }

        // 4. Check if already a member of the club
        if (memberRepository.findByClubIdAndUserId(batch.getClub().getId(), application.getUser().getId())
                .isPresent()) {
            throw new BusinessException(40916, "您已经是该社团成员，无需重复申请");
        }

        // 5. Set full batch object to application
        application.setBatch(batch);

        applicationRepository.save(application);
    }

    @Override
    public List<RecruitApplication> getApplicationsByBatch(Long batchId, Long operatorId) {
        RecruitBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("未发现招新批次"));
        permissionService.checkClubAdmin(operatorId, batch.getClub().getId());
        return applicationRepository.findByBatchId(batchId);
    }

    @Override
    public Page<RecruitApplication> getApplicationsByBatch(Long batchId, Long operatorId, int page, int size) {
        RecruitBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("未发现招新批次"));
        permissionService.checkClubAdmin(operatorId, batch.getClub().getId());
        return applicationRepository.findByBatchIdOrderByCreatedAtDesc(batchId, PageRequest.of(page, size));
    }

    @Override
    public List<RecruitApplication> getMyApplications(Long userId) {
        return applicationRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void reviewApplicationFirst(Long applicationId, boolean pass, String comment, Long operatorId) {
        RecruitApplication app = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new RuntimeException("未发现申请"));

        permissionService.checkClubAdmin(operatorId, app.getBatch().getClub().getId());
        permissionService.checkClubActive(app.getBatch().getClub().getId());

        if (!"PENDING".equals(app.getFirstReviewStatus())) {
            throw new BusinessException(40912, "该申请的一审已处理，请勿重复操作");
        }

        app.setFirstReviewStatus(pass ? "PASSED" : "REJECTED");
        app.setFirstReviewComment(comment);
        applicationRepository.save(app);
        metricsService.recordApprovalHandled("recruit_first", pass ? "approved" : "rejected", app.getCreatedAt());

        // Send notification
        try {
            NotificationMessageDTO message = new NotificationMessageDTO();
            message.setUserId(app.getUser().getId());
            message.setTitle("招新初审结果");
            message.setContent(pass ? "恭喜你已通过 " + app.getBatch().getClub().getName() + " 的初审。"
                    : "很遗憾，你未通过 " + app.getBatch().getClub().getName() + " 的初审。原因：" + comment);
            message.setType("SYSTEM");
            rabbitTemplate.convertAndSend(RabbitConstants.NOTIFICATION_EXCHANGE,
                    RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY, message);
        } catch (Exception e) {
            // Log error but don't fail transaction
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public void reviewApplicationFinal(Long applicationId, boolean pass, String comment, Long operatorId) {
        RecruitApplication app = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new RuntimeException("未发现申请"));

        permissionService.checkClubAdmin(operatorId, app.getBatch().getClub().getId());
        permissionService.checkClubActive(app.getBatch().getClub().getId());

        if (!"PASSED".equals(app.getFirstReviewStatus())) {
            throw new BusinessException(40913, "该申请一审未通过，无法进行终审");
        }
        if (!"PENDING".equals(app.getFinalReviewStatus())) {
            throw new BusinessException(40914, "该申请终审已处理，请勿重复操作");
        }

        if (pass) {
            // Lock Batch to check quota safely
            RecruitBatch batch = batchRepository.findByIdForUpdate(app.getBatch().getId())
                    .orElseThrow(() -> new RuntimeException("未发现招新批次"));

            if (batch.getQuota() != null && batch.getQuota() > 0) {
                long approvedCount = applicationRepository.countByBatchIdAndFinalReviewStatus(batch.getId(), "PASSED");
                if (approvedCount >= batch.getQuota()) {
                    throw new BusinessException(40915, "招新名额已满，无法继续通过");
                }
            }
        }

        app.setFinalReviewStatus(pass ? "PASSED" : "REJECTED");
        app.setFinalReviewComment(comment);

        if (pass) {
            // 使用 ClubService 的标准方法添加成员
            boolean alreadyMember = memberRepository.findByClubIdAndUserId(
                    app.getBatch().getClub().getId(), app.getUser().getId()).isPresent();
            if (!alreadyMember) {
                clubService.addMember(
                        app.getBatch().getClub().getId(),
                        app.getUser().getId(),
                        "MEMBER");
            }
        }

        applicationRepository.save(app);
        metricsService.recordApprovalHandled("recruit_final", pass ? "approved" : "rejected", app.getCreatedAt());

        // Send notification
        try {
            NotificationMessageDTO message = new NotificationMessageDTO();
            message.setUserId(app.getUser().getId());
            message.setTitle("招新终审结果");
            message.setContent(pass ? "恭喜你已通过终审，成功加入 " + app.getBatch().getClub().getName() + "。"
                    : "很遗憾，你未通过 " + app.getBatch().getClub().getName() + " 的终审。原因：" + comment);
            message.setType("SYSTEM");
            rabbitTemplate.convertAndSend(RabbitConstants.NOTIFICATION_EXCHANGE,
                    RabbitConstants.COMMON_NOTIFICATION_ROUTING_KEY, message);
        } catch (Exception e) {
            // Log error but don't fail transaction
            e.printStackTrace();
        }
    }

    private RecruitFormField copyFieldForBatch(RecruitFormField source, RecruitBatch targetBatch) {
        RecruitFormField copied = new RecruitFormField();
        if (source.getId() != null) {
            copied.setId(-source.getId());
        }
        copied.setBatch(targetBatch);
        copied.setFieldKey(source.getFieldKey());
        copied.setLabel(source.getLabel());
        copied.setType(source.getType());
        copied.setOptions(source.getOptions());
        copied.setRequired(source.getRequired());
        copied.setSortOrder(source.getSortOrder());
        return copied;
    }

    private List<RecruitFormField> buildDefaultFields(RecruitBatch batch) {
        RecruitFormField reason = new RecruitFormField();
        reason.setId(-1L);
        reason.setBatch(batch);
        reason.setFieldKey("reason");
        reason.setLabel("申请理由");
        reason.setType("TEXTAREA");
        reason.setRequired(true);
        reason.setSortOrder(1);

        RecruitFormField experience = new RecruitFormField();
        experience.setId(-2L);
        experience.setBatch(batch);
        experience.setFieldKey("experience");
        experience.setLabel("相关经历");
        experience.setType("TEXTAREA");
        experience.setRequired(false);
        experience.setSortOrder(2);

        return List.of(reason, experience);
    }

    private int compareBatchOrder(RecruitBatch left, RecruitBatch right, java.time.LocalDateTime now) {
        int leftRank = batchRank(left, now);
        int rightRank = batchRank(right, now);
        if (leftRank != rightRank) {
            return Integer.compare(leftRank, rightRank);
        }

        java.time.LocalDateTime leftAnchor = batchAnchorTime(left, now);
        java.time.LocalDateTime rightAnchor = batchAnchorTime(right, now);
        if (leftAnchor == null && rightAnchor == null) {
            return 0;
        }
        if (leftAnchor == null) {
            return 1;
        }
        if (rightAnchor == null) {
            return -1;
        }
        return rightAnchor.compareTo(leftAnchor);
    }

    private int batchRank(RecruitBatch batch, java.time.LocalDateTime now) {
        if (now.isBefore(batch.getStartTime())) {
            return 2;
        }
        if (now.isAfter(batch.getEndTime())) {
            return 1;
        }
        return 0;
    }

    private java.time.LocalDateTime batchAnchorTime(RecruitBatch batch, java.time.LocalDateTime now) {
        if (now.isBefore(batch.getStartTime())) {
            return batch.getStartTime();
        }
        if (now.isAfter(batch.getEndTime())) {
            return batch.getEndTime();
        }
        return batch.getStartTime();
    }
}
