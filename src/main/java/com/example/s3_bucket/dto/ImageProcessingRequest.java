package com.example.s3_bucket.dto;

import lombok.*;

@Data
@Builder
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class ImageProcessingRequest {
    private String image_key;
    private String bucket;
    private String cloudFrontUrl;
}
