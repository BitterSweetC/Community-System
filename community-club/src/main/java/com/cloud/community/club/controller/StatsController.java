package com.cloud.community.club.controller;

import com.cloud.community.core.common.Result;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        
        // Optimized: Count directly in DB
        stats.put("activeClubs", clubRepository.countByStatus("ACTIVE"));
        
        // 2. Club Status Distribution
        // Optimized: Group By in DB
        Map<String, Long> clubStatusDistribution = new HashMap<>();
        List<Object[]> statusCounts = clubRepository.countStatusDistribution();
        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            Number count = (Number) row[1];
            clubStatusDistribution.put(status, count.longValue());
        }
        stats.put("clubStatusDistribution", clubStatusDistribution);
        
        // 3. User Growth (Last 7 Days)
        // Optimized: Group By in DB
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> userGrowth = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        // Initialize last 7 days
        for (int i = 6; i >= 0; i--) {
            userGrowth.put(now.minusDays(i).format(formatter), 0L);
        }
        
        List<Object[]> growthCounts = userRepository.countUserGrowth(now.minusDays(7));
        for (Object[] row : growthCounts) {
            String dateStr = (String) row[0]; // "MM-dd" from SQL
            Number count = (Number) row[1];
            if (userGrowth.containsKey(dateStr)) {
                userGrowth.put(dateStr, count.longValue());
            }
        }
        stats.put("userGrowth", userGrowth);

        return Result.success(stats);
    }

    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_ADMIN', 'USER')")
    public Result<Map<String, Object>> getClubStats(@PathVariable Long clubId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 1. Basic counts
        // Optimized: Count directly in DB
        long memberCount = memberRepository.countByClubId(clubId);
        long activityCount = activityRepository.countByClubId(clubId);
        
        stats.put("memberCount", memberCount);
        stats.put("activityCount", activityCount);
        
        // 2. Role Distribution
        // Optimized: Group By in DB
        Map<String, Long> roleDistribution = new HashMap<>();
        List<Object[]> roleCounts = memberRepository.countRoleDistribution(clubId);
        for (Object[] row : roleCounts) {
            String role = (String) row[0];
            Number count = (Number) row[1];
            roleDistribution.put(role, count.longValue());
        }
        stats.put("roleDistribution", roleDistribution);
        
        // 3. Activity Status (Upcoming vs Past)
        LocalDateTime now = LocalDateTime.now();
        // Optimized: Count directly in DB
        long upcomingActivities = activityRepository.countByClubIdAndStartTimeAfter(clubId, now);
        long pastActivities = activityCount - upcomingActivities;
        
        Map<String, Long> activityStats = new HashMap<>();
        activityStats.put("upcoming", upcomingActivities);
        activityStats.put("past", pastActivities);
        stats.put("activityStats", activityStats);

        // 4. Recent Joins (Last 7 days)
        // Optimized: Group By in DB
        Map<String, Long> recentJoins = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        // Initialize last 7 days with 0
        for (int i = 6; i >= 0; i--) {
            recentJoins.put(now.minusDays(i).format(formatter), 0L);
        }
        
        List<Object[]> joinCounts = memberRepository.countMemberJoinTrend(clubId, now.minusDays(7));
        for (Object[] row : joinCounts) {
            String dateStr = (String) row[0];
            Number count = (Number) row[1];
            if (recentJoins.containsKey(dateStr)) {
                recentJoins.put(dateStr, count.longValue());
            }
        }
                
        stats.put("recentJoins", recentJoins);
        
        return Result.success(stats);
    }
}
