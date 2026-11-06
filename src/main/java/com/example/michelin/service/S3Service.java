package com.example.michelin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Service
public class S3Service {

    @Value("${s3.url}")
    private String s3url;

    @Value("${s3.access-key-id}")
    private String s3AccessKeyId;

    @Value("${s3.secret-key}")
    private String s3SecretKey;

    @Value("${s3.bucket-restaurant}")
    private String restaurantBucket;

    @Value("${s3.bucket-evaluation}")
    private String evaluationBucket;

    // URL présignée pour le bucket restaurant
    public String getUploadFileUrl(String key) {
        return getUploadFileUrl(restaurantBucket, key);
    }

    // Génération de l'URL présignée pour l'upload direct
    public String getUploadFileUrl(String bucket, String key) {
        try (var s3Presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3AccessKeyId, s3SecretKey)))
                .endpointOverride(URI.create(s3url))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build()) {

            PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build())
                    .build();

            return s3Presigner.presignPutObject(request)
                    .url()
                    .toExternalForm();
        }
    }

    // URL finale de l'image stockée
    public String getPublicUrl(String bucket, String key) {
        return s3url + "/" + bucket + "/" + key;
    }

    public String getRestaurantBucket() {
        return restaurantBucket;
    }

    public String getEvaluationBucket() {
        return evaluationBucket;
    }
}
