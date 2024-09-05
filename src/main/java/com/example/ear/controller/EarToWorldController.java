package com.example.ear.controller;

import com.example.ear.service.EarToWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j @RequestMapping("/api")
public class EarToWorldController {
    private final EarToWorldService earToWorldService;
    @PostMapping("/ear-to-world")
    public void earToWorld(@RequestPart("imageFile") MultipartFile imageFile) throws IOException {
        earToWorldService.mainLogic(imageFile);
    }
}
