package com.cloud.community.core.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityCheckInExportVO {
    @ExcelProperty("Activity Name")
    private String activityName;

    @ExcelProperty("Student ID")
    private String studentId;

    @ExcelProperty("Name")
    private String realName;

    @ExcelProperty("Sign Time")
    private String signTime;

    @ExcelProperty("Source")
    private String source;
}
