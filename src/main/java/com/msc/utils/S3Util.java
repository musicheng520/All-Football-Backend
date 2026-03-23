package com.msc.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Component
public class S3Util {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public S3Util() {
        this.s3Client = S3Client.builder()
                .region(Region.EU_WEST_1) // Ireland
                .build();
    }

    /**
     * 上传头像
     */
    public String uploadAvatar(MultipartFile file) throws IOException {

        String key = "avatars/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        return upload(file, key);
    }

    /**
     * 上传新闻封面
     */
    public String uploadNewsCover(MultipartFile file) throws IOException {

        String key = "news/cover/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        return upload(file, key);
    }

    /**
     * 上传新闻内容图片
     */
    public String uploadNewsContent(MultipartFile file) throws IOException {

        String key = "news/content/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        return upload(file, key);
    }

    /**
     * 核心上传方法
     */
    private String upload(MultipartFile file, String key) throws IOException {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromBytes(file.getBytes())
        );

        // 返回访问URL
        return "https://" + bucket + ".s3.amazonaws.com/" + key;
    }
}