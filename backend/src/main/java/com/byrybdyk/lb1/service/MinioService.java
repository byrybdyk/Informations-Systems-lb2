package com.byrybdyk.lb1.service;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class MinioService {
    private final MinioClient minioClient;
    private final String bucketName;

    public MinioService(
            @Value("${minio.url}")
            String url,
            @Value("${minio.access.name}") String accessKey,
            @Value("${minio.access.secret}") String secretKey,
            @Value("${minio.bucket.name}") String bucketName) {
        this.minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
        this.bucketName = bucketName;

    }

    public void uploadFile(String objectName, InputStream stream, long size, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(stream, size, -1)
                        .contentType(contentType)
                        .build()
        );
    }


    public void rollbackMinioFile(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
            System.out.println("Файл удален из MinIO: " + objectName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String generateUrl(String bucketName, String fileName) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .expiry(60 * 60)
                    .method(Method.GET)
                    .build();

            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while generating presigned URL", e);
        }
    }

    public void deleteFile(String minioFilePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(minioFilePath).build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while deleting file", e);
        }
    }

    public void copyFile(String tempMinioFilePath, String finalMinioFilePath) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder().bucket(bucketName).object(tempMinioFilePath).build())
                            .bucket(bucketName)
                            .object(finalMinioFilePath)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while copying file", e);
        }
    }
}