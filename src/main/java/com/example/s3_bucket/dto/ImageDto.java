package com.example.s3_bucket.dto;

import lombok.*;

@Data
@Builder
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class ImageDto {

    private String imageUrl;
    private String encryptUrl;

}
