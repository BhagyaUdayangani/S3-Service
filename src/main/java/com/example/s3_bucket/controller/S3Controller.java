package com.example.s3_bucket.controller;

import com.example.s3_bucket.dto.*;
import com.example.s3_bucket.service.S3Service;
import com.example.s3_bucket.annotation.CurrentUser;
import com.example.s3_bucket.annotation.TrackExecutionTime;
import com.example.s3_bucket.enums.CommonMessages;
import com.example.s3_bucket.enums.ImageType;
import com.example.s3_bucket.enums.StatusType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

@Slf4j
@Validated
@RestController
@RequestMapping("${s3.api.header}")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;
    private final RestTemplate restTemplate;

    private static final String TOKEN_ERROR_MESSAGE = "Token should not be empty";
    private static final String FILE_PARAM = "file";
    private static final String IMAGE_URL_PARAM = "imageUrl";
    private static final String IMAGE_TYPE_PARAM = "imageType";
    private static final String AUTH_HEADER = "Authorization";

    @Value("${s3.api.end-point.update-URL}")
    private String imageServiceUrl;

    private static ResponseEntity<CommonResponse> handleTokenError() {
        CommonResponse commonResponse = CommonResponse.builder()
                .data(TOKEN_ERROR_MESSAGE)
                .meta(new MetaData(true, CommonMessages.INTERNAL_SERVER_ERROR, 500, "Token is not recognized"))
                .status(StatusType.STATUS_FAIL)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(commonResponse);
    }

    private static ResponseEntity<CommonResponse> handleError(String message, String details) {
        CommonResponse commonResponse = CommonResponse.builder()
                .data(message)
                .meta(new MetaData(true, CommonMessages.INTERNAL_SERVER_ERROR, 500, details))
                .status(StatusType.STATUS_FAIL)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(commonResponse);
    }

    private static boolean isPrincipalValid(@NonNull Principal principal) {
        return Optional.ofNullable(principal.getName())
                .filter(name -> !name.isEmpty())
                .isEmpty();
    }

    @TrackExecutionTime
    @PostMapping("${s3.api.end-point.upload}")
    public ResponseEntity<CommonResponse> uploadFile(
            @RequestPart(FILE_PARAM) MultipartFile file,
            @CurrentUser Principal principal,
            @RequestHeader(AUTH_HEADER) String token) {
        
        log.info("Processing image upload request");
        
        if (isPrincipalValid(principal)) {
            log.warn("Invalid principal detected during file upload");
            return handleTokenError();
        }

        try {
            CommonResponse response = s3Service.uploadFile(file, principal.getName(), token, ImageType.POST);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            return handleError(e.getMessage(), "Error in image upload");
        }
    }

    @TrackExecutionTime
    @PostMapping("${s3.api.end-point.uploadV2}")
    public ResponseEntity<CommonResponse> uploadFile(@RequestPart(value = "file") MultipartFile file,
                                                     @RequestPart(value = "imageType") String imageType,
                                                     @CurrentUser Principal principal,
                                                     @RequestHeader("Authorization") String token) {
        try {
            log.info("LOG :: Image upload v2 API is called!");
            if (isPrincipalValid(principal)) {
                return handleTokenError();
            }
            return new ResponseEntity<>(s3Service.uploadFile(file, principal.getName(), token, ImageType.valueOf(imageType)), HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResponse.builder()
                    .data(e.getMessage())
                    .meta(new MetaData(true, CommonMessages.INTERNAL_SERVER_ERROR,500,"error in image upload"))
                    .status(StatusType.STATUS_FAIL)
                    .build());
        }
    }

    @TrackExecutionTime
    @PostMapping("${s3.api.end-point.update}")
    public ResponseEntity<CommonResponse> updateFile(
            @RequestPart(FILE_PARAM) MultipartFile file,
            @RequestPart(IMAGE_URL_PARAM) String s3ImageUrl,
            @RequestPart(IMAGE_TYPE_PARAM) String imageType,
            @RequestHeader(AUTH_HEADER) String token,
            @CurrentUser Principal principal) {

        log.info("Processing image update request for URL: {}", s3ImageUrl);

        if (isPrincipalValid(principal)) {
            log.warn("Invalid principal detected during file update");
            return handleTokenError();
        }

        try {
            ImageType imageType1 = ImageType.valueOf(imageType);
            CommonResponse s3Response = s3Service.updatePhoto(s3ImageUrl, file, principal.getName(), token, imageType1);
            
            String imageUrl;
            Object data = s3Response.getData();
            if (data instanceof ImageDto) {
                imageUrl = ((ImageDto) data).getImageUrl();
            } else if (data instanceof String) {
                imageUrl = (String) data;
            } else {
                log.error("Unexpected data type received from S3 service: {}", data != null ? data.getClass() : "null");
                return handleError("Unexpected data from S3 service", "Data type mismatch");
            }

            ImageRequestDto imageRequest = ImageRequestDto.builder()
                    .imageUrl(imageUrl)
                    .imageType(imageType1)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(AUTH_HEADER, token);

            HttpEntity<ImageRequestDto> requestEntity = new HttpEntity<>(imageRequest, headers);
            log.info("Forwarding image update request to image service at: {}", imageServiceUrl);

            return restTemplate.postForEntity(imageServiceUrl, requestEntity, CommonResponse.class);

        } catch (IOException e) {
            log.error("Error occurred during file update", e);
            return handleError(e.getMessage(), "Error in image update");
        } catch (IllegalArgumentException e) {
            log.error("Invalid image type provided: {}", imageType, e);
            return handleError("Invalid image type", "The provided image type is not supported");
        } catch (Exception e) {
            log.error("An unexpected error occurred during file update", e);
            return handleError("Unexpected error", "An unexpected error occurred during the image update process");
        }
    }
}