package com.cloud.community.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "t_recruit_form_field")
public class RecruitFormField extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RecruitBatch batch;

    @Column(name = "field_key", nullable = false, length = 50)
    private String fieldKey;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(columnDefinition = "json")
    private String options;

    private Boolean required = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
