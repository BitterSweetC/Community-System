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
    public Result<Map<String, Long>> getSystemStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalClubs", clubRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalActivities", activityRepository.count());
        // Count pending club approvals if possible, or other global metrics
        stats.put("activeClubs", clubRepository.findAll().stream()
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .count());
        return Result.success(stats);
    }

    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_ADMIN', 'STUDENT')") // Relaxed for now, or strictly check membership
    public Result<Map<String, Object>> getClubStats(@PathVariable Long clubId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic counts
        long memberCount = memberRepository.findByClubId(clubId).size(); // Optimized count query would be better but this works
        long activityCount = activityRepository.findByClubId(clubId).size();
        
        stats.put("memberCount", memberCount);
        stats.put("activityCount", activityCount);
        
        // Add more specific stats if needed (e.g., gender ratio, recruitment stats)
        
        return Result.success(stats);
    }
}
