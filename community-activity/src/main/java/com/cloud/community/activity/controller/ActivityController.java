package com.cloud.community.activity.controller;

import com.cloud.community.core.common.PageResult;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.entity.ActivitySignup;
import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.model.dto.ActivityCreateDTO;
import com.cloud.community.core.model.dto.ActivityUpdateDTO;
import com.cloud.community.core.entity.ActivityAttendance;
import com.cloud.community.core.model.vo.ActivityCheckInExportVO;
import com.cloud.community.core.model.vo.ActivityRewardSettlementVO;
import com.cloud.community.core.model.vo.ActivityVO;
import com.cloud.community.core.model.vo.MySignupActivityVO;
import com.alibaba.excel.EasyExcel;
import com.cloud.community.activity.service.ActivityService;
import com.cloud.community.user.service.PermissionService;
import com.cloud.community.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final UserService userService;
    private final PermissionService permissionService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return userService.findByUsername(((UserDetails) principal).getUsername()).orElseThrow();
        }
        throw new AuthenticationCredentialsNotFoundException("Not authenticated");
    }

    @PostMapping
    public Result<ActivityVO> createActivity(@Validated @RequestBody ActivityCreateDTO dto) {
        User user = getCurrentUser();
        if (dto.getClubId() != null) {
            permissionService.checkClubAdmin(user.getId(), dto.getClubId());
            Activity activity = new Activity();
            BeanUtils.copyProperties(dto, activity);
            Club club = new Club();
            club.setId(dto.getClubId());
            activity.setClub(club);
            return Result.success(ActivityVO.from(activityService.createActivity(activity, dto.getResourceApplicationId())));
        }

        permissionService.checkSystemAdmin(user.getId());
        Activity activity = new Activity();
        BeanUtils.copyProperties(dto, activity);
        return Result.success(ActivityVO.from(activityService.createActivity(activity, dto.getResourceApplicationId())));
    }

    @GetMapping
    public Result<PageResult<ActivityVO>> getAllActivities(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String clubName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (clubId == null
                && !StringUtils.hasText(keyword)
                && !StringUtils.hasText(clubName)
                && !StringUtils.hasText(startDate)
                && !StringUtils.hasText(endDate)) {
            return Result.success(PageResult.of(activityService.getAllActivities(page, size)).map(ActivityVO::from));
        }

        LocalDateTime startTimeFrom = null;
        LocalDateTime startTimeTo = null;
        if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (end.isBefore(start)) {
                throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
            }
            startTimeFrom = start.atStartOfDay();
            startTimeTo = end.plusDays(1).atStartOfDay().minusNanos(1);
        }

        return Result.success(PageResult.of(activityService.getActivities(
                clubId, keyword, clubName, startTimeFrom, startTimeTo, page, size)).map(ActivityVO::from));
    }

    @GetMapping("/club/{clubId}")
    public Result<PageResult<ActivityVO>> getClubActivities(@PathVariable Long clubId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return Result.success(PageResult.of(activityService.getActivitiesByClub(clubId, page, size)).map(ActivityVO::from));
    }

    @GetMapping("/{id}")
    public Result<ActivityVO> getActivity(@PathVariable Long id) {
        return Result.success(ActivityVO.from(activityService.getActivityById(id)));
    }

    @PostMapping("/{id}/signup")
    public Result<Void> signup(@PathVariable Long id) {
        User user = getCurrentUser();
        activityService.signup(id, user.getId());
        return Result.success();
    }

    @PostMapping("/{id}/signin")
    public Result<Void> signIn(@PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        User user = getCurrentUser();
        String code = body != null ? body.get("code") : null;
        activityService.signIn(id, user.getId(), code);
        return Result.success();
    }

    @GetMapping("/{id}/signups")
    public Result<List<ActivitySignup>> getSignups(@PathVariable Long id) {
        User user = getCurrentUser();
        Activity activity = activityService.getActivityById(id);
        if (activity.getClub() != null) {
            permissionService.checkClubAdmin(user.getId(), activity.getClub().getId());
        } else {
            permissionService.checkSystemAdmin(user.getId());
        }
        return Result.success(activityService.getSignups(id));
    }

    @GetMapping("/{id}/checkins/export")
    public void exportCheckIns(@PathVariable Long id, HttpServletResponse response) throws IOException {
        User user = getCurrentUser();
        Activity activity = activityService.getActivityById(id);
        if (activity.getClub() != null) {
            permissionService.checkClubAdmin(user.getId(), activity.getClub().getId());
        } else {
            permissionService.checkSystemAdmin(user.getId());
        }

        List<ActivityAttendance> attendances = activityService.getAttendances(id);

        List<ActivityCheckInExportVO> exportList = attendances.stream().map(a -> {
            ActivityCheckInExportVO vo = new ActivityCheckInExportVO();
            vo.setActivityName(a.getActivity().getTitle());
            vo.setStudentId(a.getUser().getUsername());
            vo.setRealName(a.getUser().getRealName());
            vo.setSignTime(a.getSignTime().toString());
            vo.setSource(a.getSource());
            return vo;
        }).toList();

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("Activity_CheckIns", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), ActivityCheckInExportVO.class)
                .sheet("CheckIns")
                .doWrite(exportList);

        response.getOutputStream().flush();
    }

    @GetMapping("/my-signups")
    public Result<List<MySignupActivityVO>> getMySignups() {
        User user = getCurrentUser();
        List<ActivitySignup> signups = activityService.getUserSignups(user.getId());
        return Result.success(signups.stream().map(MySignupActivityVO::from).toList());
    }

    @PutMapping("/{id}")
    public Result<ActivityVO> updateActivity(@PathVariable Long id, @Validated @RequestBody ActivityUpdateDTO dto) {
        User user = getCurrentUser();
        Activity activity = activityService.getActivityById(id);
        if (activity.getClub() != null) {
            permissionService.checkClubAdmin(user.getId(), activity.getClub().getId());
        } else {
            permissionService.checkSystemAdmin(user.getId());
        }
        return Result.success(ActivityVO.from(activityService.updateActivity(id, dto)));
    }

    @PostMapping("/{id}/settle-rewards")
    public Result<ActivityRewardSettlementVO> settleRewards(@PathVariable Long id) {
        User user = getCurrentUser();
        Activity activity = activityService.getActivityById(id);
        if (activity.getClub() != null) {
            permissionService.checkClubAdmin(user.getId(), activity.getClub().getId());
        } else {
            permissionService.checkSystemAdmin(user.getId());
        }
        return Result.success(activityService.settleRewards(id, user.getId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        User user = getCurrentUser();
        Activity activity = activityService.getActivityById(id);
        if (activity.getClub() != null) {
            permissionService.checkClubAdmin(user.getId(), activity.getClub().getId());
        } else {
            permissionService.checkSystemAdmin(user.getId());
        }
        activityService.deleteActivity(id);
        return Result.success();
    }
}
