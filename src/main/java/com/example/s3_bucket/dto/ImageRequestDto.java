package com.example.s3_bucket.dto;

import lombok.*;
import com.example.s3_bucket.enums.ImageType;

@Data
@Builder
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class ImageRequestDto {

    private String userName;
    private String imageUrl;
    private ImageType imageType;

}

