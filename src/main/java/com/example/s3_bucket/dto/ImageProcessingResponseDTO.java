package com.example.s3_bucket.dto;

import lombok.*;

@Data
@Builder
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class ImageProcessingResponseDTO {
    private String status;
    private Urls urls;

    @Data
    public static class Urls {
        private String profile;
        private String square;
        private String portrait;
        private String landscape;
        private String story;
        private String reelCoverSafeZone;
        private String thumbnail;
    }
}