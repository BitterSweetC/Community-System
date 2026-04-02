package com.cloud.community.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "t_resource_application")
public class ResourceApplication extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false, insertable = false, updatable = false)
    private Club club;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activity_id", insertable = false, updatable = false)
    private Activity activity;

    @Column(name = "activity_id")
    private Long activityId;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resource_id", nullable = false, insertable = false, updatable = false)
    private Resource resource;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "approver_id")
    private Long approverId;

    @Transient
    private String clubName;
}
