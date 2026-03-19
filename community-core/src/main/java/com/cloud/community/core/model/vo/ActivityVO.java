package com.cloud.community.core.model.vo;

import com.cloud.community.core.entity.Activity;
import com.cloud.community.core.entity.Club;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class ActivityVO {
    private Long id;
    private Long clubId;
    private String clubName;
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
    private Integer rewardPoints;
    private String settlementStatus;
    private LocalDateTime settledAt;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ActivityVO from(Activity activity) {
        if (activity == null) {
            return null;
        }
        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(activity, vo);
        try {
            if (activity.getClub() != null) {
                vo.setClubId(activity.getClub().getId());
                vo.setClubName(activity.getClub().getName());
            }
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // Club reference exists but entity is missing
            vo.setClubName("未知社团");
        } catch (Exception e) {
            // Handle other potential proxy errors
            vo.setClubName("未知社团");
        }
        return vo;
    }
}
