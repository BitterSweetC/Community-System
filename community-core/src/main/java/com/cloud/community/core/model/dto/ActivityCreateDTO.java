package com.cloud.community.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityCreateDTO {
    @NotNull(message = "所属社团不能为空")
    private Long clubId;

    @NotBlank(message = "活动标题不能为空")
    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;

    @Size(max = 5000, message = "描述不能超过5000个字符")
    private String description;

    private String coverUrl;

    @NotBlank(message = "活动类型不能为空")
    private String type;

    @Size(max = 255, message = "地点不能超过255个字符")
    private String location;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private LocalDateTime signupStartTime;
    private LocalDateTime signupEndTime;
    private Integer maxParticipants;
    private Boolean needAttendance;
    private String checkinCode;
    private Integer rewardPoints;
    private Long resourceApplicationId;
}
