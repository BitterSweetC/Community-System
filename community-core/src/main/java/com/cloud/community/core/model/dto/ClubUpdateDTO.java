package com.cloud.community.core.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;

@Data
public class ClubUpdateDTO {
    private Long id;

    @Size(max = 100, message = "社团名称不能超过100个字符")
    private String name;

    @Size(max = 50, message = "简称不能超过50个字符")
    private String shortName;

    @Size(max = 50, message = "分类不能超过50个字符")
    private String category;

    private String logoUrl;

    @Size(max = 2000, message = "简介不能超过2000个字符")
    private String description;

    private Set<String> tags;
}
