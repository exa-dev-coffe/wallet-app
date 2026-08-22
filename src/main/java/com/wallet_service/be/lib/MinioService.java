package com.wallet_service.be.lib;

import com.wallet_service.be.exception.BadRequestException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class MinioService {

    private final MinioClient minioClient;
    private final String bucketName;
    private final String url;

    public MinioService(
            @Value("${minio.url}") String url,
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.accessKey}") String accessKey,
            @Value("${minio.secretKey}") String secretKey,
            @Value("${minio.bucketName}") String bucketName
    ) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucketName = bucketName;
        this.url = url;
    }

    // Upload file
    public String uploadFile(MultipartFile file, String objectName) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        // URL akses file
        return url + "/" + bucketName + "/" + objectName;
    }

    // Delete file
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error delete file: {}", e.getMessage());
            throw new BadRequestException("Failed to delete file");
        }
    }
}