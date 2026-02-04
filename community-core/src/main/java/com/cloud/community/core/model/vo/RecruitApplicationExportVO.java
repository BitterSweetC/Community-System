package com.cloud.community.core.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecruitApplicationExportVO {
    @ExcelProperty("Recruitment Batch")
    private String batchName;

    @ExcelProperty("Student ID")
    private String studentId;

    @ExcelProperty("Name")
    private String realName;

    @ExcelProperty("First Review Status")
    private String firstReviewStatus;

    @ExcelProperty("Final Review Status")
    private String finalReviewStatus;

    @ExcelProperty("Application Time")
    private String createTime;
}
