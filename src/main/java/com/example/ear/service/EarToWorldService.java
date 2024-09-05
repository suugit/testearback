package com.example.ear.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.ear.dto.request.ChatGptRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import static com.example.ear.dto.request.ChatGptRequest.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EarToWorldService {

    private static final String COMMNET = "위 내용을 장애인과 노인들이 이해할 수 있도록 어려운 말들은 " +
            "부가설명을 해주고 전체적인 내용을 쉽고 자세하게 구어체로 요약해줘";

    @Value("${cloud.aws.credentials.access-key}")
    private String s3AccessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String s3SecretKey;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${naver.service.secretKey}")
    private String naverOcrSecretKey;

    private final ChatGPTService chatGPTService;
    private final NaverOrcApiService naverOrcApiService;
    private final AmazonS3Client amazonS3Client;



    private S3Client s3Client() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(s3AccessKey, s3SecretKey);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    public void mainLogic(MultipartFile imageFile) throws IOException {
        // 1. S3 에 이미지 파일 저장 후 이미지 URL 가져오기
        String imageUrl = getImageUrlFromS3(imageFile);

        // 2. 가져온 이미지 URL 을 네이버 OCR 에 보내서 내용 추출하기
        String extractContent = naverOrcApiService.callApi("POST", imageUrl, naverOcrSecretKey, "jpg");

        // 2-1) 추출하기 위해 프롬프트에 내용을 추가
        extractContent = extractContent + "\n" + COMMNET;

        // 3. 추출한 내용을 ChatGPT 에게 보내서 요약본 요청하기
        String summaryResultFromChatGPT = chatGPTService.prompt(SummaryDto.of(extractContent));

        // 4. 받아온 요약본을 AWS Polly 에 보내서 음성 파일 가져오기

        // 5. 응답 받은 음성 파일을 클라이언트에게 반환하기

    }

    private String getImageUrlFromS3(MultipartFile imageFile) throws IOException {
        //파일의 원본 이름
        String originalFileName = imageFile.getOriginalFilename();

        //DB에 저장될 파일 이름
        String storeFileName = createStoreFileName(originalFileName);

        // S3 에 저장
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(imageFile.getContentType());
        metadata.setContentLength(imageFile.getSize());
        amazonS3Client.putObject(bucket, storeFileName, imageFile.getInputStream(), metadata);

        return getFileUrl(storeFileName);
    }

    private String createStoreFileName(String originalFilename) {
        String ext = extractExt(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + ext;
    }
    /**
     * 파일 확장자를 추출하기 위해 만든 메서드
     */
    private String extractExt(String originalFilename) {
        int post = originalFilename.lastIndexOf(".");
        return originalFilename.substring(post + 1);
    }

    public String getFileUrl(String fileName) {
        S3Client s3 = s3Client();
        // 객체의 URL을 가져오는 메서드
        URL url = s3.utilities().getUrl(GetUrlRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build());
        return url.toString();
    }


}
