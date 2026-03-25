package com.cloud.community.notice.controller;

import com.cloud.community.core.common.PageResult;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.Notice;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.repository.ClubRepository;
import com.cloud.community.core.repository.NoticeRepository;
import com.cloud.community.core.repository.UserRepository;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.core.model.dto.NotificationMessageDTO;
import com.cloud.community.core.constant.RabbitConstants;
import com.cloud.community.notice.service.NoticeModerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
// @CrossOrigin(origins = "*") // Handled in SecurityConfig
public class NoticeController {

    private final NoticeRepository noticeRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final RabbitTemplate rabbitTemplate;
    private final NoticeModerationService noticeModerationService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/{id}")
    public Result<Notice> getNotice(@PathVariable Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notice not found"));
        enrichNoticeDisplayFields(List.of(notice));
        return Result.success(notice);
    }

    @GetMapping
    public Result<PageResult<Notice>> getNotices(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        String normalizedTitle = StringUtils.hasText(title) ? title.trim() : null;

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (StringUtils.hasText(startDate)) {
            startTime = LocalDate.parse(startDate).atStartOfDay();
        }
        if (StringUtils.hasText(endDate)) {
            endTime = LocalDate.parse(endDate).plusDays(1).atStartOfDay().minusNanos(1);
        }
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }

        Page<Notice> noticePage = noticeRepository.searchPublished(
                clubId, "PUBLISHED", normalizedTitle, startTime, endTime, pageRequest);
        enrichNoticeDisplayFields(noticePage.getContent());
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
        noticeModerationService.validateForPublish(notice);
        notice.setPublishedBy(user.getId());
        notice.setStatus("PUBLISHED"); // Default to published for simplicity
        notice.setPublishedAt(java.time.LocalDateTime.now());
        Notice savedNotice = noticeRepository.save(notice);

        // 发布公告后通过 RabbitMQ 推送通知
        try {
            NotificationMessageDTO message = new NotificationMessageDTO();
            message.setTitle("新公告：" + savedNotice.getTitle());
            message.setContent(savedNotice.getTitle());
            if (savedNotice.getClubId() != null) {
                message.setClubId(savedNotice.getClubId());
                message.setType("CLUB");
                rabbitTemplate.convertAndSend(RabbitConstants.NOTIFICATION_EXCHANGE, RabbitConstants.CLUB_BROADCAST_ROUTING_KEY, message);
            } else {
                message.setType("SYSTEM");
                rabbitTemplate.convertAndSend(RabbitConstants.NOTIFICATION_EXCHANGE, RabbitConstants.CLUB_BROADCAST_ROUTING_KEY, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        enrichNoticeDisplayFields(List.of(savedNotice));
        return Result.success(savedNotice);
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

    private void enrichNoticeDisplayFields(List<Notice> notices) {
        if (notices == null || notices.isEmpty()) {
            return;
        }

        Map<Long, Club> clubMap = loadClubMap(notices);
        Map<Long, User> publisherMap = loadPublisherMap(notices);

        for (Notice notice : notices) {
            Club club = notice.getClubId() == null ? null : clubMap.get(notice.getClubId());
            if (club != null) {
                notice.setClubName(club.getName());
                notice.setPublisherName(club.getName());
            }

            if (!StringUtils.hasText(notice.getPublisherName())) {
                User publisher = notice.getPublishedBy() == null ? null : publisherMap.get(notice.getPublishedBy());
                if (publisher != null) {
                    String displayName = StringUtils.hasText(publisher.getRealName())
                            ? publisher.getRealName()
                            : publisher.getUsername();
                    notice.setPublisherName(displayName);
                }
            }

            if (!StringUtils.hasText(notice.getPublisherName())) {
                notice.setPublisherName("系统管理员");
            }
        }
    }

    private Map<Long, Club> loadClubMap(List<Notice> notices) {
        List<Long> clubIds = notices.stream()
                .map(Notice::getClubId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (clubIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return StreamSupport.stream(clubRepository.findAllById(clubIds).spliterator(), false)
                .collect(Collectors.toMap(Club::getId, Function.identity()));
    }

    private Map<Long, User> loadPublisherMap(List<Notice> notices) {
        List<Long> publisherIds = notices.stream()
                .map(Notice::getPublishedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (publisherIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return StreamSupport.stream(userRepository.findAllById(publisherIds).spliterator(), false)
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
