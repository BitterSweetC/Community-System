package com.cloud.community.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "t_resource")
public class Resource extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String type; // VENUE, MATERIAL

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String location; // Only for VENUE

    @Column
    private Integer capacity; // Only for VENUE

    @Column(name = "total_quantity")
    private Integer totalQuantity; // Only for MATERIAL

    @Column(nullable = false, length = 20)
    private String status = "AVAILABLE"; // AVAILABLE, MAINTENANCE
}
