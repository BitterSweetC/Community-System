package com.cloud.community.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "t_club")
public class Club extends BaseEntity {

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

    @ElementCollection
    @CollectionTable(name = "t_club_tag", joinColumns = @JoinColumn(name = "club_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();
}
