package com.cloud.community.gateway.controller;

import com.cloud.community.core.common.Result;
import com.cloud.community.core.repository.ActivityAttendanceRepository;
import com.cloud.community.core.repository.ActivitySignupRepository;
import com.cloud.community.core.repository.ClubRepository;
import com.cloud.community.core.repository.ResourceApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ClubRepository clubRepository;
    private final ActivityAttendanceRepository attendanceRepository;
    private final ActivitySignupRepository signupRepository;
    private final ResourceApplicationRepository resourceApplicationRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CLUB_ADMIN')")
    public Result<Map<String, Object>> getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        long activeClubs = clubRepository.countByStatus("ACTIVE");
        long todayCheckins = attendanceRepository.countBySignTimeBetween(todayStart, todayEnd);
        long pendingResources = resourceApplicationRepository.countByStatus("PENDING");
        long totalSignups = signupRepository.count();

        return Result.success(Map.of(
                "activeClubs", activeClubs,
                "todayCheckins", todayCheckins,
                "pendingResources", pendingResources,
                "totalSignups", totalSignups
        ));
    }
}
