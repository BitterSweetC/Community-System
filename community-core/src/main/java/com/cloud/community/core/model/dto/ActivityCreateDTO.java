package com.cloud.community.core.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityCreateDTO {
    private Long clubId;
    private String title;
    private String description;
    private String coverUrl;
    private String type;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime signupStartTime;
    private LocalDateTime signupEndTime;
    private Integer maxParticipants;
    private Boolean needAttendance;
    private String checkinCode;
}
