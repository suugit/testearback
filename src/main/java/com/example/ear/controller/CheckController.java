package com.example.ear.controller;

import com.example.ear.service.NaverOrcApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class CheckController {
    private final NaverOrcApiService naverApi;

    @Value("${naver.service.secretKey}")
    private String secretKey;

    @GetMapping("/naverOcr")
    public ResponseEntity ocr() throws IOException {

        String filePath = "https://www.kbiznews.co.kr/news/photo/202001/62528_21538_5322.jpg";
        String result = naverApi.callApi("POST", filePath, secretKey, "jpg");
        if (!result.equals(null)) {
            log.info("result = {}" , result);
        } else {
            log.info("null");
        }
        return new ResponseEntity(result, HttpStatus.OK);
    }
}