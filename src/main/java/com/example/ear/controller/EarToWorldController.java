package com.example.ear.controller;

import com.example.ear.service.EarToWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j @RequestMapping("/api")
public class EarToWorldController {
    private final EarToWorldService earToWorldService;
    @PostMapping("/ear-to-world")
    public ResponseEntity<byte[]> earToWorld(@RequestPart("imageFile") MultipartFile imageFile) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "audio/mpeg"); // 적절한 MIME 타입 설정
        // inline 은 다운로드 하지 않고, 페이지 내에서 바로 재생 처리
        headers.add("Content-Disposition", "inline; filename=speech.mp3");
        return new ResponseEntity<>(earToWorldService.mainLogic(imageFile).toByteArray(),headers, HttpStatus.OK);
    }
}
