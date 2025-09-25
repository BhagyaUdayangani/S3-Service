package com.example.s3_bucket.service;

import com.example.s3_bucket.dto.CommonResponse;
import com.example.s3_bucket.enums.ImageType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface S3Service {

    CommonResponse uploadFile(MultipartFile multipartFile, String authUserId, String token, ImageType imageType) throws IOException;

    CommonResponse updatePhoto(String imageUrl,MultipartFile multipartFile, String authUserId, String token, ImageType imageType)  throws IOException;

}
