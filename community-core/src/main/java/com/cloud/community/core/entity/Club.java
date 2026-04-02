package com.cloud.community.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "t_club")
public class Club extends BaseEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISSOLVING = "DISSOLVING";
    public static final String STATUS_DISSOLVED = "DISSOLVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "dissolution_reason")
    private String dissolutionReason;

    @Column(name = "dissolution_date")
    private LocalDateTime dissolutionDate;

    @Column(name = "visit_count")
    private Integer visitCount = 0;

    @Column(name = "balance", precision = 10, scale = 2)
    private java.math.BigDecimal balance = java.math.BigDecimal.ZERO;

    @Transient
    private Long memberCount = 0L;

    @Transient
    private Long activityCount = 0L;

    @Transient
    private String presidentName;

    @ElementCollection
    @CollectionTable(name = "t_club_tag", joinColumns = @JoinColumn(name = "club_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();
}
