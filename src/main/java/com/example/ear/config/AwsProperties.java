package com.example.ear.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloud.aws")
public class AwsProperties {
    private Credentials credentials;
    private String region;
    private S3 s3;

    // Getters and Setters

    public static class Credentials {
        private String accessKey;
        private String secretKey;
        // Getters and Setters
    }

    public static class S3 {
        private String bucket;
        // Getters and Setters
    }
}
