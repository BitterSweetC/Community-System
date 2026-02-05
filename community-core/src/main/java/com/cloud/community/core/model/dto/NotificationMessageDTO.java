package com.cloud.community.core.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class NotificationMessageDTO implements Serializable {
    private Long userId;
    private Long clubId;
    private String title;
    private String content;
    private String type;
}
