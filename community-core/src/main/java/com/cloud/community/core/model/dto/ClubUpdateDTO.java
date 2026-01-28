package com.cloud.community.core.model.dto;

import lombok.Data;
import java.util.Set;

@Data
public class ClubUpdateDTO {
    private Long id;
    private String name;
    private String shortName;
    private String category;
    private String logoUrl;
    private String description;
    private Set<String> tags;
}
