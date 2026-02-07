package com.cloud.community.core.model.vo;

import com.cloud.community.core.entity.AuditLog;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;

    public static AuditLogVO from(AuditLog log, String username, String realName) {
        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(log, vo);
        vo.setUsername(username);
        vo.setRealName(realName);
        return vo;
    }
}
