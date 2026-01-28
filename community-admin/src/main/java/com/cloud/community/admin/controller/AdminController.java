package com.cloud.community.admin.controller;

import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.club.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.cloud.community.core.model.vo.ClubVO;
import com.cloud.community.core.entity.Club;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')") // Will be enabled later
public class AdminController {

    private final ClubService clubService;
    private final UserRepository userRepository;

    @GetMapping("/clubs/pending")
    public Result<List<ClubVO>> getPendingClubs() {
        List<Club> clubs = clubService.getPendingClubs();
        return Result.success(clubs.stream().map(ClubVO::from).collect(Collectors.toList()));
    }

    @PostMapping("/clubs/{id}/approve")
    public Result<Void> approveClub(@PathVariable Long id) {
        clubService.approveClub(id);
        return Result.success();
    }
    
    @DeleteMapping("/clubs/{id}")
    public Result<Void> deleteClub(@PathVariable Long id) {
        clubService.deleteClub(id);
        return Result.success();
    }

    @PostMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(status);
        userRepository.save(user);
        return Result.success();
    }
}
