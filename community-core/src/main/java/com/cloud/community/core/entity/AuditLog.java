package com.cloud.community.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "t_audit_log")
public class AuditLog extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 50)
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(length = 50)
    private String ip;
}
