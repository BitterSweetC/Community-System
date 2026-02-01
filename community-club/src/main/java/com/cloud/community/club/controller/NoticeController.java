package com.cloud.community.club.controller;

import com.cloud.community.core.common.PageResult;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.Notice;
import com.cloud.community.core.repository.NoticeRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.core.entity.User;
import com.cloud.community.club.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
// @CrossOrigin(origins = "*") // Handled in SecurityConfig
public class NoticeController {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public Result<PageResult<Notice>> getNotices(
            @RequestParam(required = false) Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        
        if (clubId != null) {
            Page<Notice> noticePage = noticeRepository.findByClubIdAndStatus(clubId, "PUBLISHED", pageRequest);
            return Result.success(PageResult.of(noticePage));
        }
        // Return all published notices (including CLUB scope) to ensure content is visible on home page
        Page<Notice> noticePage = noticeRepository.findByStatus("PUBLISHED", pageRequest);
        return Result.success(PageResult.of(noticePage));
    }

    @PostMapping
    public Result<Notice> createNotice(@RequestBody Notice notice) {
        User user = getCurrentUser();
        if (notice.getClubId() != null) {
            permissionService.checkClubAdmin(user.getId(), notice.getClubId());
            permissionService.checkClubActive(notice.getClubId());
        } else {
            permissionService.checkSystemAdmin(user.getId());
        }
        notice.setPublishedBy(user.getId());
        notice.setStatus("PUBLISHED"); // Default to published for simplicity
        return Result.success(noticeRepository.save(notice));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        User user = getCurrentUser();
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notice not found"));
        
        if (notice.getClubId() != null) {
            permissionService.checkClubAdmin(user.getId(), notice.getClubId());
            permissionService.checkClubActive(notice.getClubId());
        } else {
            permissionService.checkSystemAdmin(user.getId());
        }
        
        noticeRepository.deleteById(id);
        return Result.success();
    }
}
