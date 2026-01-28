package com.cloud.community.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "t_recruit_application")
public class RecruitApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private RecruitBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @Column(name = "apply_data", nullable = false, columnDefinition = "json")
    private String applyData;

    @Column(name = "first_review_status", length = 20)
    private String firstReviewStatus = "PENDING";

    @Column(name = "first_review_comment")
    private String firstReviewComment;

    @Column(name = "final_review_status", length = 20)
    private String finalReviewStatus = "PENDING";

    @Column(name = "final_review_comment")
    private String finalReviewComment;
}
