package com.cloud.community.recruit.controller;

import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.RecruitApplication;
import com.cloud.community.core.entity.RecruitBatch;
import com.cloud.community.core.entity.RecruitFormField;
import com.cloud.community.core.entity.User;
import com.cloud.community.recruit.service.RecruitService;
import com.cloud.community.user.service.UserService;
import com.cloud.community.core.model.vo.ClubVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recruit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecruitController {

    private final RecruitService recruitService;
    private final UserService userService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return userService.findByUsername(((UserDetails) principal).getUsername()).orElseThrow();
        }
        throw new RuntimeException("Not authenticated");
    }

    @PostMapping("/batches")
    public Result<RecruitBatch> createBatch(@RequestBody RecruitBatch batch) {
        User user = getCurrentUser();
        return Result.success(recruitService.createBatch(batch, user.getId()));
    }

    @GetMapping("/batches")
    public Result<List<RecruitBatch>> getBatches(@RequestParam Long clubId) {
        return Result.success(recruitService.getBatchesByClub(clubId));
    }
    
    @GetMapping("/batches/{id}")
    public Result<RecruitBatch> getBatch(@PathVariable Long id) {
        return Result.success(recruitService.getBatchById(id));
    }

    @PostMapping("/fields")
    public Result<Void> addFormField(@RequestBody RecruitFormField field) {
        User user = getCurrentUser();
        recruitService.addFormField(field, user.getId());
        return Result.success();
    }

    @GetMapping("/fields")
    public Result<List<RecruitFormField>> getFormFields(@RequestParam Long batchId) {
        return Result.success(recruitService.getFormFields(batchId));
    }

    @PostMapping("/applications")
    public Result<Void> submitApplication(@RequestBody RecruitApplication application) {
        User user = getCurrentUser();
        application.setUser(user);
        recruitService.submitApplication(application);
        return Result.success();
    }

    @GetMapping("/applications")
    public Result<List<RecruitApplication>> getApplications(@RequestParam(required = false) Long batchId) {
        User user = getCurrentUser();
        if (batchId != null) {
            return Result.success(recruitService.getApplicationsByBatch(batchId, user.getId()));
        }
        return Result.success(recruitService.getMyApplications(user.getId()));
    }

    @PostMapping("/applications/{id}/first-review")
    public Result<Void> firstReview(@PathVariable Long id, @RequestParam boolean pass, @RequestParam String comment) {
        User user = getCurrentUser();
        recruitService.reviewApplicationFirst(id, pass, comment, user.getId());
        return Result.success();
    }

    @PostMapping("/applications/{id}/final-review")
    public Result<Void> finalReview(@PathVariable Long id, @RequestParam boolean pass, @RequestParam String comment) {
        User user = getCurrentUser();
        recruitService.reviewApplicationFinal(id, pass, comment, user.getId());
        return Result.success();
    }

    @GetMapping("/active-clubs")
    public Result<List<ClubVO>> getRecruitingClubs() {
        return Result.success(recruitService.getRecruitingClubs().stream()
                .map(ClubVO::from)
                .collect(Collectors.toList()));
    }
}
