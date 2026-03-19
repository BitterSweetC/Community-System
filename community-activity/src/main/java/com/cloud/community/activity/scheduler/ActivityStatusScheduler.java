package com.cloud.community.activity.scheduler;

import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.repository.ActivityRepository;
import com.cloud.community.core.service.MemberArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityStatusScheduler {

    private final ActivityRepository activityRepository;
    private final MemberArchiveService memberArchiveService;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(fixedDelay = 60_000)
    public void markInProgress() {
        int count = executeInTransaction(() -> activityRepository.markInProgress(LocalDateTime.now()));
        if (count > 0) {
            log.info("活动状态推进：{} 个活动变为 IN_PROGRESS", count);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void markEnded() {
        int count = executeInTransaction(() -> activityRepository.markEnded(LocalDateTime.now()));
        if (count > 0) {
            log.info("活动状态推进：{} 个活动变为 ENDED", count);
        }

        for (Activity activity : activityRepository.findByStatusAndSettlementStatus("ENDED", "PENDING")) {
            try {
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transactionTemplate.executeWithoutResult(status ->
                        memberArchiveService.settleActivityRewards(activity.getId(), null));
            } catch (Exception e) {
                log.warn("活动奖励结算失败，activityId={}", activity.getId(), e);
            }
        }
    }

    private int executeInTransaction(Supplier<Integer> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Integer result = transactionTemplate.execute(status -> action.get());
        return result == null ? 0 : result;
    }
}
