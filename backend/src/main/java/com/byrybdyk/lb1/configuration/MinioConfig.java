package com.byrybdyk.lb1.configuration;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class MinioConfig {
    @Value("${minio.bucket.name}")
    private String minioBuscketName;
    @Value("${minio.url}")
    private String minioUrl;
    @Value("${minio.access.name}")
    private String minioAccessKey;
    @Value("${minio.access.secret}")
    private String minioSecretKey;
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }
}