package com.example.ear.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.example.ear.config.AwsProperties;
import com.example.ear.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.VoiceId;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@Service
@Slf4j
public class PollyService {

    // private final AwsProperties awsProperties;

    @Value(value = "${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;
    private PollyClient pollyClient;

    public PollyService() {
        // AWS 자격 증명 설정
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        // PollyClient 인스턴스 생성
        this.pollyClient = PollyClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    public ResponseInputStream<SynthesizeSpeechResponse> synthesizeSpeech(String text, String outputFileName) {
        // 음성 합성 요청 생성
        SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                .text(text)
                .outputFormat(OutputFormat.MP3) // 출력 형식 설정 (MP3 또는 OGG_VORBIS 등)
                .voiceId(VoiceId.JOANNA) // 사용할 음성 설정
                .build();

        // Polly API 호출하여 음성 파일 생성
        return pollyClient.synthesizeSpeech(request);
        }
    }

