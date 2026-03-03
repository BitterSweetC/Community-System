package com.cloud.community.club.controller;

import com.cloud.community.club.service.FinanceService;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.ClubFinance;
import com.cloud.community.core.entity.User;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;
    private final UserService userService;
    private final PermissionService permissionService;

    private User getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null ||
            !SecurityContextHolder.getContext().getAuthentication().isAuthenticated() ||
            "anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
            throw new RuntimeException("User not authenticated");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/transactions")
    public Result<ClubFinance> createTransaction(@RequestBody ClubFinance transaction) {
        User user = getCurrentUser();
        if (transaction.getClubId() == null) {
            throw new RuntimeException("Club ID is required");
        }
        permissionService.checkClubAdmin(user.getId(), transaction.getClubId());
        transaction.setApplicantId(user.getId());
        return Result.success(financeService.createTransaction(transaction));
    }

    @GetMapping("/clubs/{clubId}/transactions")
    public Result<List<ClubFinance>> getClubTransactions(@PathVariable Long clubId) {
        User user = getCurrentUser();
        permissionService.checkClubAdmin(user.getId(), clubId);
        return Result.success(financeService.getClubTransactions(clubId));
    }

    @GetMapping("/clubs/{clubId}/balance")
    public Result<BigDecimal> getClubBalance(@PathVariable Long clubId) {
        User user = getCurrentUser();
        permissionService.checkClubAdmin(user.getId(), clubId);
        return Result.success(financeService.getClubBalance(clubId));
    }

    @PostMapping("/transactions/{id}/approve")
    public Result<Void> approveTransaction(@PathVariable Long id) {
        User user = getCurrentUser();
        permissionService.checkSystemAdmin(user.getId());
        financeService.approveTransaction(id, user.getId());
        return Result.success(null);
    }

    @PostMapping("/transactions/{id}/reject")
    public Result<Void> rejectTransaction(@PathVariable Long id) {
        User user = getCurrentUser();
        permissionService.checkSystemAdmin(user.getId());
        financeService.rejectTransaction(id, user.getId());
        return Result.success(null);
    }
}
