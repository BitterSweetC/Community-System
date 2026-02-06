package com.cloud.community.activity.controller;

import com.cloud.community.core.common.PageResult;
import com.cloud.community.core.common.Result;
import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.entity.ActivitySignup;
import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.User;
import com.cloud.community.core.model.dto.ActivityCreateDTO;
import com.cloud.community.core.entity.ActivityAttendance;
import com.cloud.community.core.model.vo.ActivityCheckInExportVO;
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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ActivityController {

    private final ActivityService activityService;
    private final UserService userService;
    private final PermissionService permissionService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return userService.findByUsername(((UserDetails) principal).getUsername()).orElseThrow();
        }
        throw new RuntimeException("Not authenticated");
    }

    @PostMapping
    public Result<ActivityVO> createActivity(@RequestBody ActivityCreateDTO dto) {
        User user = getCurrentUser();
        if (dto.getClubId() != null) {
            permissionService.checkClubAdmin(user.getId(), dto.getClubId());
            Activity activity = new Activity();
            BeanUtils.copyProperties(dto, activity);
            Club club = new Club();
            club.setId(dto.getClubId());
            activity.setClub(club);
            return Result.success(ActivityVO.from(activityService.createActivity(activity)));
        }
        // If clubId is null, maybe system activity? Assuming club activity for now as per requirement.
        // If user didn't provide clubId, logic might fail or it's a global activity (Admin only).
        // Let's assume system admin if clubId is null.
        permissionService.checkSystemAdmin(user.getId());
        Activity activity = new Activity();
        BeanUtils.copyProperties(dto, activity);
        return Result.success(ActivityVO.from(activityService.createActivity(activity)));
    }

    @GetMapping
    public Result<PageResult<ActivityVO>> getAllActivities(
            @RequestParam(required = false) Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (clubId != null) {
            return Result.success(PageResult.of(activityService.getActivitiesByClub(clubId, page, size)).map(ActivityVO::from));
        }
        return Result.success(PageResult.of(activityService.getAllActivities(page, size)).map(ActivityVO::from));
    }

    @GetMapping("/club/{clubId}")
    public Result<List<ActivityVO>> getClubActivities(@PathVariable Long clubId) {
        return Result.success(activityService.getActivitiesByClub(clubId).stream().map(ActivityVO::from).toList());
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
    public Result<Void> signIn(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
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
