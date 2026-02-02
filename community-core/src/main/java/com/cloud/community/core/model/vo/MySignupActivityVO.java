package com.cloud.community.core.model.vo;

import com.cloud.community.core.entity.ActivitySignup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class MySignupActivityVO extends ActivityVO {
    private String signupStatus;
    private LocalDateTime signupTime;

    public static MySignupActivityVO from(ActivitySignup signup) {
        if (signup == null) {
            return null;
        }
        MySignupActivityVO vo = new MySignupActivityVO();
        
        // Convert Activity to ActivityVO first to handle club mapping logic safely
        ActivityVO activityVO = ActivityVO.from(signup.getActivity());
        if (activityVO != null) {
            BeanUtils.copyProperties(activityVO, vo);
        }
        
        vo.setSignupStatus(signup.getStatus());
        vo.setSignupTime(signup.getCreatedAt());
        return vo;
    }
}
