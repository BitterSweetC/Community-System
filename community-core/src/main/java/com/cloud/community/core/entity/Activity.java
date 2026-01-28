package com.cloud.community.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "t_activity")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Activity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Club club;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(nullable = false, length = 50)
    private String type;

    private String location;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "signup_start_time")
    private LocalDateTime signupStartTime;

    @Column(name = "signup_end_time")
    private LocalDateTime signupEndTime;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "need_attendance")
    private Boolean needAttendance = false;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";
}
