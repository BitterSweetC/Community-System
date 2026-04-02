package com.cloud.community.core.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityUpdateDTO {
    private Long id;

    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;

    @Size(max = 5000, message = "描述不能超过5000个字符")
    private String description;

    private String coverUrl;
    private String type;

    @Size(max = 255, message = "地点不能超过255个字符")
    private String location;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime signupStartTime;
    private LocalDateTime signupEndTime;
    private Integer maxParticipants;
    private Boolean needAttendance;
    private String checkinCode;
    private Integer rewardPoints;
    private Long resourceApplicationId;
}
