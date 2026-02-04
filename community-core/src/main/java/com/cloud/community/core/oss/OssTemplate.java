package com.cloud.community.core.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OssTemplate {

    private final OSS ossClient;
    private final OssProperties ossProperties;

    /**
     * 上传文件并返回访问 URL
     * @param file 前端上传的文件
     * @return 文件访问路径
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File cannot be empty");
        }

        // 1. 生成唯一文件名: yyyy/MM/dd/uuid.jpg
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
        String fileName = UUID.randomUUID().toString() + extension;
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String datePath = sdf.format(new Date());
        
        String objectName = datePath + "/" + fileName;

        // 2. 上传到 OSS
        try {
            // 创建 PutObjectRequest 对象
            // 必须设置 ACL 为 PublicRead，否则私有 Bucket 无法直接访问头像
            // 必须设置 ContentType，否则浏览器可能无法正确预览图片
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectAcl(CannedAccessControlList.PublicRead);
            if (file.getContentType() != null) {
                metadata.setContentType(file.getContentType());
            }

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    objectName,
                    file.getInputStream(),
                    metadata
            );
            
            ossClient.putObject(putObjectRequest);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to OSS", e);
        }

        // 3. 拼接返回 URL
        // 确保 urlPrefix 以 / 结尾，objectName 不以 / 开头
        String prefix = ossProperties.getUrlPrefix();
        if (prefix != null && !prefix.endsWith("/")) {
            prefix += "/";
        }
        String fullUrl = prefix + objectName;
        log.info("File uploaded successfully. URL: {}", fullUrl);
        return fullUrl;
    }
}
