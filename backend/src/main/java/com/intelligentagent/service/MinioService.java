package com.intelligentagent.service;

import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import com.intelligentagent.config.MinioProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioService {

    private final MinioProperties properties;
    private final MinioClient minioClient;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    public MinioService(MinioProperties properties, MinioClient minioClient) {
        this.properties = properties;
        this.minioClient = minioClient;
        ensureBucket();
    }

    private void ensureBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucketName())
                    .build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.getBucketName())
                        .build());
                log.info("Created MinIO bucket: {}", properties.getBucketName());
            } else {
                log.info("MinIO bucket '{}' already exists", properties.getBucketName());
            }
            setPublicReadPolicy();
        } catch (Exception e) {
            log.error("MinIO bucket check/create failed: {}", e.getMessage());
        }
    }

    private void setPublicReadPolicy() {
        try {
            String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(properties.getBucketName());

            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(properties.getBucketName())
                    .config(policy)
                    .build());
            log.info("Set public read policy for bucket: {}", properties.getBucketName());
        } catch (Exception e) {
            log.warn("Failed to set public read policy (may already be set): {}", e.getMessage());
        }
    }

    public byte[] downloadBytes(String url) {
        try {
            Request httpRequest = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("下载音频失败: HTTP " + response.code());
                }
                if (response.body() == null) {
                    throw new RuntimeException("下载音频失败: 响应体为空");
                }
                return response.body().bytes();
            }
        } catch (Exception e) {
            throw new RuntimeException("下载音频失败: " + e.getMessage(), e);
        }
    }

    public String uploadFromUrl(String sourceUrl, String directory) {
        try {
            log.info("Downloading audio from: {}...", sourceUrl.substring(0, Math.min(sourceUrl.length(), 80)));

            byte[] data = downloadBytes(sourceUrl);
            String contentType = guessContentType(data);
            log.info("Downloaded audio: {} bytes, detectedContentType={}", data.length, contentType);

            return uploadBytes(data, directory, contentType);
        } catch (Exception e) {
            log.error("Failed to upload audio to MinIO: {}", e.getMessage(), e);
            return sourceUrl;
        }
    }

    public String uploadBytes(byte[] data, String directory, String contentType) {
        try {
            String extension = guessExtension(contentType);
            String fileName = directory + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

            try (InputStream inputStream = new ByteArrayInputStream(data)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucketName())
                        .object(fileName)
                        .stream(inputStream, data.length, -1)
                        .contentType(contentType)
                        .build());
            }

            String publicUrl = properties.getPublicUrl() + "/" + properties.getBucketName() + "/" + fileName;
            log.info("Uploaded audio to MinIO: {} ({} bytes, {})", publicUrl, data.length, contentType);
            return publicUrl;
        } catch (Exception e) {
            log.error("Failed to upload audio to MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("上传音频到MinIO失败: " + e.getMessage(), e);
        }
    }

    private String guessContentType(byte[] data) {
        if (data != null && data.length >= 4) {
            String header = new String(data, 0, 4);
            if ("RIFF".equals(header)) {
                return "audio/wav";
            }
        }
        if (data != null && data.length >= 3) {
            if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFB) {
                return "audio/mpeg";
            }
            if (data[0] == 0x49 && data[1] == 0x44 && data[2] == 0x33) {
                return "audio/mpeg";
            }
        }
        return "audio/wav";
    }

    private String guessExtension(String contentType) {
        if (contentType == null) return ".wav";
        if (contentType.contains("mpeg") || contentType.contains("mp3")) return ".mp3";
        if (contentType.contains("ogg")) return ".ogg";
        if (contentType.contains("flac")) return ".flac";
        return ".wav";
    }
}
