package com.cloud.community.user.controller;

import com.cloud.community.core.common.Result;
import com.cloud.community.core.metrics.BusinessMetricsService;
import com.cloud.community.core.oss.OssTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final OssTemplate ossTemplate;
    private final BusinessMetricsService metricsService;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            String url = ossTemplate.upload(file);
            metricsService.recordUploadSuccess();
            return Result.success(url);
        } catch (RuntimeException ex) {
            metricsService.recordUploadFailure();
            throw ex;
        }
    }
}
