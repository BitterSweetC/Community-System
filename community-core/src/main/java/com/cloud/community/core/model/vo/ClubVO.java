package com.cloud.community.core.model.vo;

import com.cloud.community.core.entity.Club;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ClubVO {
    private Long id;
    private String name;
    private String shortName;
    private String category;
    private String logoUrl;
    private String description;
    private Integer foundedYear;
    private String status;
    private Long createdBy;
    private Set<String> tags;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long memberCount;
    private Long activityCount;

    public static ClubVO from(Club club) {
        if (club == null) {
            return null;
        }
        ClubVO vo = new ClubVO();
        BeanUtils.copyProperties(club, vo);
        vo.setMemberCount(club.getMemberCount());
        vo.setActivityCount(club.getActivityCount());
        return vo;
    }
}
