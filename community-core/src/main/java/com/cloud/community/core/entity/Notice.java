package com.cloud.community.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "t_notice")
public class Notice extends BaseEntity {

    @Column(name = "club_id")
    private Long clubId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String scope;

    @Column(name = "published_by", nullable = false)
    private Long publishedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Transient
    private String clubName;

    @Transient
    private String publisherName;
}
