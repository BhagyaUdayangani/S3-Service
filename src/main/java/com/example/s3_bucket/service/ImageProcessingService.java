package com.example.s3_bucket.service;

import com.example.s3_bucket.dto.ImageProcessingResponseDTO;

public interface ImageProcessingService {

    ImageProcessingResponseDTO processImage(String imageKey);

}
