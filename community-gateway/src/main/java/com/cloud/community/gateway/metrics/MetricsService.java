package com.cloud.community.gateway.metrics;

import com.cloud.community.core.repository.ActivityAttendanceRepository;
import com.cloud.community.core.repository.ClubRepository;
import com.cloud.community.core.repository.ResourceApplicationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final ClubRepository clubRepository;
    private final ActivityAttendanceRepository attendanceRepository;
    private final ResourceApplicationRepository resourceApplicationRepository;

    private Counter loginCounter;
    private Counter loginFailCounter;
    private Counter checkinCounter;
    private Counter resourceApplyCounter;

    @PostConstruct
    public void init() {
        loginCounter = Counter.builder("community.login.success")
                .description("Total successful logins")
                .register(meterRegistry);

        loginFailCounter = Counter.builder("community.login.failure")
                .description("Total failed login attempts")
                .register(meterRegistry);

        checkinCounter = Counter.builder("community.activity.checkin")
                .description("Total activity check-ins")
                .register(meterRegistry);

        resourceApplyCounter = Counter.builder("community.resource.apply")
                .description("Total resource applications submitted")
                .register(meterRegistry);

        // Gauges — read live from DB
        io.micrometer.core.instrument.Gauge.builder("community.clubs.active",
                        clubRepository, r -> r.countByStatus("ACTIVE"))
                .description("Number of active clubs")
                .register(meterRegistry);

        io.micrometer.core.instrument.Gauge.builder("community.resource.pending",
                        resourceApplicationRepository, r -> r.countByStatus("PENDING"))
                .description("Number of pending resource applications")
                .register(meterRegistry);
    }

    public void recordLoginSuccess() {
        loginCounter.increment();
    }

    public void recordLoginFailure() {
        loginFailCounter.increment();
    }

    public void recordCheckin() {
        checkinCounter.increment();
    }

    public void recordResourceApply() {
        resourceApplyCounter.increment();
    }
}
