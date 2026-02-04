package com.cloud.community.core.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClubMemberExportVO {
    @ExcelProperty("Club Name")
    private String clubName;

    @ExcelProperty("Student ID")
    private String studentId;

    @ExcelProperty("Name")
    private String realName;

    @ExcelProperty("Role")
    private String role;

    @ExcelProperty("Status")
    private String status;

    @ExcelProperty("Join Time")
    private String joinTime;
}
