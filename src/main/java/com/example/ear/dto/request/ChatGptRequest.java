package com.example.ear.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatGptRequest {

    @Builder @Getter
    @NoArgsConstructor @AllArgsConstructor
    public static class SummaryDto {
        private String content;

        // dummy
        // TODO: 9/5/24 수정 사항
        private Long memberId = 1L;

        public static SummaryDto of(String content) {
            return SummaryDto.builder()
                    .content(content)
                    .memberId(1L)
                    .build();
        }
    }
}
