package com.cloud.community.club.controller;

import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.Club;
import com.cloud.community.core.repository.ActivityRepository;
import com.cloud.community.core.repository.ClubRepository;
import com.cloud.community.core.repository.MemberRepository;
import com.cloud.community.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 1. Basic Counts
        stats.put("totalClubs", clubRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalActivities", activityRepository.count());
        
        long activeClubs = clubRepository.findAll().stream()
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .count();
        stats.put("activeClubs", activeClubs);
        
        // 2. Club Status Distribution
        Map<String, Long> clubStatusDistribution = clubRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Club::getStatus,
                        java.util.stream.Collectors.counting()
                ));
        stats.put("clubStatusDistribution", clubStatusDistribution);
        
        // 3. User Growth (Last 7 Days) - Assuming created_at exists on User, otherwise mock or skip
        // Note: User entity extends BaseEntity so it has getCreatedAt()
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Map<String, Long> userGrowth = new java.util.TreeMap<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MM-dd");
        
        // Initialize last 7 days
        for (int i = 6; i >= 0; i--) {
            userGrowth.put(now.minusDays(i).format(formatter), 0L);
        }
        
        // This is inefficient for large datasets, but acceptable for demo/small scale
        // In production, use a native query with group by date
        userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(now.minusDays(7)))
                .forEach(u -> {
                    String dateKey = u.getCreatedAt().format(formatter);
                    userGrowth.put(dateKey, userGrowth.getOrDefault(dateKey, 0L) + 1);
                });
        stats.put("userGrowth", userGrowth);

        return Result.success(stats);
    }

    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_ADMIN', 'STUDENT')")
    public Result<Map<String, Object>> getClubStats(@PathVariable Long clubId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 1. Basic counts
        java.util.List<com.cloud.community.core.entity.Member> members = memberRepository.findByClubId(clubId);
        java.util.List<com.cloud.community.core.entity.Activity> activities = activityRepository.findByClubId(clubId);
        
        long memberCount = members.size();
        long activityCount = activities.size();
        
        stats.put("memberCount", memberCount);
        stats.put("activityCount", activityCount);
        
        // 2. Role Distribution
        Map<String, Long> roleDistribution = members.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        com.cloud.community.core.entity.Member::getRoleCode,
                        java.util.stream.Collectors.counting()
                ));
        stats.put("roleDistribution", roleDistribution);
        
        // 3. Activity Status (Upcoming vs Past)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long upcomingActivities = activities.stream()
                .filter(a -> a.getStartTime().isAfter(now))
                .count();
        long pastActivities = activityCount - upcomingActivities;
        
        Map<String, Long> activityStats = new HashMap<>();
        activityStats.put("upcoming", upcomingActivities);
        activityStats.put("past", pastActivities);
        stats.put("activityStats", activityStats);

        // 4. Recent Joins (Last 7 days)
        Map<String, Long> recentJoins = new java.util.TreeMap<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MM-dd");
        
        // Initialize last 7 days with 0
        for (int i = 6; i >= 0; i--) {
            recentJoins.put(now.minusDays(i).format(formatter), 0L);
        }
        
        members.stream()
                .filter(m -> m.getJoinAt() != null && m.getJoinAt().isAfter(now.minusDays(7)))
                .forEach(m -> {
                    String dateKey = m.getJoinAt().format(formatter);
                    recentJoins.put(dateKey, recentJoins.getOrDefault(dateKey, 0L) + 1);
                });
                
        stats.put("recentJoins", recentJoins);
        
        return Result.success(stats);
    }
}
