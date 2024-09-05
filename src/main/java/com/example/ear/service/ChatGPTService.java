package com.example.ear.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.ear.config.ChatGPTConfig;
import com.example.ear.config.S3Config;
import com.example.ear.dto.request.ChatGPTRequestDto;
import com.example.ear.dto.request.ChatGptRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static com.example.ear.dto.request.ChatGPTRequestDto.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatGPTService {
    private final ChatGPTConfig chatGPTConfig;
    private final AmazonS3Client amazonS3Client;

    @Value("${openai.url.prompt}")
    private String promptUrl;

    @Value("${openai.url.image-prompt}")
    private String imagePromptUrl;

    @Value("${openai.url.model}")
    private String modelUrl;

    @Value(("${openai.secret-key}"))
    private String secretKey;

    @Value("${cloud.aws.credentials.access-key}")
    private String s3AccessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String s3SecretKey;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;


    private final ObjectMapper om = new ObjectMapper();

    private S3Client s3Client() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(s3AccessKey, s3SecretKey);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }



    public List<Map<String, Object>> modelList() {
        log.debug("[+] 모델 리스트를 조회합니다.");
        List<Map<String, Object>> resultList = null;

        // [STEP1] 토큰 정보가 포함된 Header를 가져옵니다.
        HttpHeaders headers = chatGPTConfig.httpHeaders();

        // [STEP2] 통신을 위한 RestTemplate을 구성합니다.
        ResponseEntity<String> response = chatGPTConfig
                .restTemplate()
                .exchange(modelUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        try {
            // [STEP3] Jackson을 기반으로 응답값을 가져옵니다.
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> data = om.readValue(response.getBody(), new TypeReference<>() {
            });

            // [STEP4] 응답 값을 결과값에 넣고 출력을 해봅니다.
            resultList = (List<Map<String, Object>>) data.get("data");
            for (Map<String, Object> object : resultList) {
                log.debug("ID: " + object.get("id"));
                log.debug("Object: " + object.get("object"));
                log.debug("Created: " + object.get("created"));
                log.debug("Owned By: " + object.get("owned_by"));
            }
        } catch (JsonMappingException e) {
            log.debug("JsonMappingException :: " + e.getMessage());
        } catch (JsonProcessingException e) {
            log.debug("JsonProcessingException :: " + e.getMessage());
        } catch (RuntimeException e) {
            log.debug("RuntimeException :: " + e.getMessage());
        }
        return resultList;
    }

    public String prompt(MultipartFile imageFile) throws IOException {
        //파일의 원본 이름
        String originalFileName = imageFile.getOriginalFilename();

        //DB에 저장될 파일 이름
        String storeFileName = createStoreFileName(originalFileName);

        // S3 에 저장
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(imageFile.getContentType());
        metadata.setContentLength(imageFile.getSize());
        amazonS3Client.putObject(bucket, storeFileName, imageFile.getInputStream(), metadata);

        // 저장한거 바로 가지고 와서 gpt 에 넘기고 그 이미지 분석한 값을 String 으로 가져오기
        String imageUrl = getFileUrl(storeFileName);


        // 요청 본문 설정
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image_url", imageUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + secretKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // API 요청
        ResponseEntity<String> response = chatGPTConfig
                .restTemplate()
                .exchange(imagePromptUrl, HttpMethod.POST, requestEntity, String.class);

        // 응답 처리
        JsonNode jsonResponse = om.readTree(response.getBody());
        log.info("jsonResponse = {} ", jsonResponse.toString());
        return "ok";
    }

    /**
     * 1. 이미지 파일을 OCR 을 통해 텍스트들을 추출합니다.
     */

    /**
     * 2. OCR 을 통해 추출한 텍스트를 Chat GPT API 를 통해 요약합니다.
     */
    public String prompt(ChatGptRequest.SummaryDto summaryDto) {

        List<ChatGPTRequestDto.ChatRequestMsgDto> message = new ArrayList<>();
        message.add(new ChatGPTRequestDto.ChatRequestMsgDto("system", summaryDto.getContent()));
        ChatGPTRequestDto.ChatCompletionDto chatCompletionDto = new ChatGPTRequestDto.ChatCompletionDto("gpt-3.5-turbo", message);

        Map<String, Object> resultMap = new HashMap<>();
        // 토큰 정보가 포함된 Header 가져오기
        HttpHeaders headers = chatGPTConfig.httpHeaders();

        // 통신을 위한 RestTemplate 구성
        HttpEntity<ChatGPTRequestDto.ChatCompletionDto> requestEntity = new HttpEntity<>(chatCompletionDto, headers);

        ResponseEntity<String> response = chatGPTConfig
                .restTemplate()
                .exchange(promptUrl, HttpMethod.POST, requestEntity, String.class);

        try {
            ObjectMapper om = new ObjectMapper();
            resultMap = om.readValue(response.getBody(), new TypeReference<>() {
            });
        } catch (JsonMappingException e) {
            log.debug("JsonMappingException :: " + e.getMessage());
        } catch (JsonProcessingException e) {
            log.debug("RuntimeException :: " + e.getMessage());
        }

        String[] split = extractGPTMessage(resultMap);
        StringBuilder sb = new StringBuilder();
        log.info("split Length = {} " , split.length);
        for (String s : split) {
            sb.append(s);
        }
        log.info("result = {} " ,  sb.toString());
        return sb.toString();
    }

    /**
     * 3. 요약한 텍스트를 AWS Polly 를 통해 음성 파일로 변환하고 클라이언트에게 반환합니다.
     */

    public String getFileUrl(String fileName) {
        S3Client s3 = s3Client();
        // 객체의 URL을 가져오는 메서드
        URL url = s3.utilities().getUrl(GetUrlRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build());
        return url.toString();
    }


    /**
     * 파일명이 겹치는 것을 방지하기위해 중복되지않는 UUID를 생성해서 반환(ext는 확장자)
     */
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

    private static String[] extractGPTMessage(Map<String, Object> resultMap) {
        ArrayList<Object> choices = (ArrayList<Object>) resultMap.get("choices");
        LinkedHashMap<String, Object> choice = (LinkedHashMap<String, Object>) choices.get(0);
        LinkedHashMap<String, Object> message = (LinkedHashMap<String, Object>) choice.get("message");
        String content = (String) message.get("content");
        String[] split = content.split("\n");
        return split;
    }
}
