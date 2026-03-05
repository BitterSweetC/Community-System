package com.cloud.community.activity.scheduler;

import com.cloud.community.core.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityStatusScheduler {

    private final ActivityRepository activityRepository;

    /**
     * 每分钟执行一次，将已到开始时间的 PUBLISHED 活动推进为 IN_PROGRESS
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void markInProgress() {
        int count = activityRepository.markInProgress(LocalDateTime.now());
        if (count > 0) {
            log.info("活动状态推进：{} 个活动变为 IN_PROGRESS", count);
        }
    }

    /**
     * 每分钟执行一次，将已过结束时间的 IN_PROGRESS 活动推进为 ENDED
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void markEnded() {
        int count = activityRepository.markEnded(LocalDateTime.now());
        if (count > 0) {
            log.info("活动状态推进：{} 个活动变为 ENDED", count);
        }
    }
}
