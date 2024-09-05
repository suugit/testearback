package com.example.ear.controller;

import com.example.ear.dto.request.ChatGPTRequestDto;
import com.example.ear.dto.request.ChatGptRequest;
import com.example.ear.service.ChatGPTService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/")
public class ChatGPTController {

    private final ChatGPTService chatGPTService;
//    @PostMapping("/summation")
//    public ResponseEntity<String> summary(@RequestPart("imageFile") MultipartFile imageFile) throws IOException {
//        return ResponseEntity.ok().body(chatGPTService.prompt(imageFile));
//    }

    @PostMapping("/summation")
    public ResponseEntity<String> summary(@RequestBody ChatGptRequest.SummaryDto summaryDto) throws IOException {
        return ResponseEntity.ok().body(chatGPTService.prompt(summaryDto));
    }
}
