package com.example.s3_bucket.service.Impl;

import com.example.s3_bucket.dto.*;
import com.example.s3_bucket.enums.ImageType;
import com.example.s3_bucket.service.ImageProcessingService;
import com.example.s3_bucket.service.S3Service;
import com.example.s3_bucket.service.UserService;
import com.example.s3_bucket.service.VideoCompressionService;
import com.example.s3_bucket.enums.CommonMessages;
import com.example.s3_bucket.enums.StatusType;
import com.example.s3_bucket.util.ValidateImageVideo;
import com.spordee.user.dto.request.PostCount;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "heic", "image/png",
            "jpg", "image/jpg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "mp4", "video/mp4",
            "avi", "video/avi",
            "mov", "video/mov"
    );

    private static final Set<String> INAPPROPRIATE_LABELS = Set.of(
            "Explicit", "Non-Explicit Nudity of Intimate parts and Kissing",
            "Violence", "Visually Disturbing", "Drugs & Tobacco", "Alcohol",
            "Rude Gestures", "Gambling", "Hate Symbols"
    );
    private final ImageProcessingService imageProcessingService;

    private S3Client s3Client;
    private RekognitionClient rekognitionClient;
    private final ValidateImageVideo validateImageVideo;

    @Value("${api.class.s3.image.access-key}")
    private String accessKey;

    @Value("${api.class.s3.image.bucket-name}")
    private String bucketName;

    @Value("${cloud-front.url}")
    private String cloudFrontUrl;

    @Value("${api.class.s3.image.secret-key}")
    private String secretKey;

    @Value("${rekognition.image-count}")
    private Integer imageCount;

    @Value("${rekognition.video-count}")
    private Integer videoCount;

    @Value("${rekognition.minimum.confidence}")
    private Float minConfidence;

    @Value("${aws.bucket.region}")
    private String region;

    private final VideoCompressionService videoCompressionService;
    private final UserService userService;

    @PostConstruct
    private void initializeAWSClients() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        log.info("LOG:: Initializing AWS credentials using region: {}", region);
        Region awsRegion = Region.of(region);
        log.info("LOG:: Initializing AWS S3 and Rekognition clients with region: {}", awsRegion);
        
        this.s3Client = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        this.rekognitionClient = RekognitionClient.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    @Override
    public CommonResponse uploadFile(MultipartFile multipartFile, String authUserId, String token, ImageType imageType) {
        try {
            PostCount oldPostCount = userService.getPostCount(authUserId, token);
//            PostCount oldPostCount = PostCount.builder().imageCount(0L).videoCount(0L).build();
            String originalFilename = validateAndGetFilename(multipartFile);
            String extension = validateAndGetExtension(originalFilename);
            log.info("LOG:: Uploading file {} with extension {}", originalFilename, extension);
            return getCommonResponse(multipartFile, oldPostCount, originalFilename, extension, imageType);
        } catch (Exception e) {
            log.error("Error during file upload", e);
            return buildFailureResponse("File upload failed: " + e.getMessage());
        }
    }

    private CommonResponse getCommonResponse(MultipartFile multipartFile, PostCount oldPostCount,
                                             String originalFilename, String extension, ImageType imageType) throws Exception {
        File tempFile = createTempFile(multipartFile, originalFilename);
        log.info("LOG:: Created temporary file {}", tempFile.getName());
        ProcessedFileInfo processedInfo = processFile(tempFile, originalFilename, extension, oldPostCount);
        try {
            if (processedInfo.isInappropriate()) {
                return handleRequestError();
            }

            String imageUrl = uploadProcessedFile(processedInfo, imageType);
            log.info("LOG:: File uploaded successfully to S3, image URL: {}", imageUrl);
            return buildSuccessResponse(imageUrl);
        } finally {
            cleanupFiles(tempFile, processedInfo.getProcessedFile());
        }
    }

    @Override
    public CommonResponse updatePhoto(String imageUrl, MultipartFile multipartFile, String authUserId, String token, ImageType imageType) {
        try {
            PostCount oldPostCount = userService.getPostCount(authUserId, token);
            String originalFilename = validateAndGetFilename(multipartFile);
            String extension = validateAndGetExtension(originalFilename);

            deleteExistingObject(imageUrl);

            return getCommonResponse(multipartFile, oldPostCount, originalFilename, extension, imageType);
        } catch (Exception e) {
            log.error("Error during photo update", e);
            return buildFailureResponse("Photo update failed: " + e.getMessage());
        }
    }

    private void deleteExistingObject(String imageUrl) {
        try {
            String key = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
        } catch (Exception e) {
            log.warn("Failed to delete existing object: {}", imageUrl, e);
        }
    }

    private String validateAndGetFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Original filename is missing");
        }
        return filename;
    }

    private String validateAndGetExtension(String filename) {
        String extension = FilenameUtils.getExtension(filename).toLowerCase();
        if (extension.isEmpty()) {
            throw new IllegalArgumentException("File extension is missing");
        }
        return extension;
    }

    private File createTempFile(MultipartFile multipartFile, String originalFilename) throws IOException {
        Path tempFilePath = Files.createTempFile("temp", originalFilename);
        File tempFile = tempFilePath.toFile();
        multipartFile.transferTo(tempFile);
        return tempFile;
    }

    private ProcessedFileInfo processFile(File tempFile, String originalFilename, String extension, PostCount postCount) throws Exception {
        log.info("LOG:: Processing file {} with extension {}", originalFilename, extension);
        ProcessedFileInfo info = new ProcessedFileInfo();
        info.setOriginalFile(tempFile);

        if (validateImageVideo.isVideo(extension)) {
            log.info("LOG:: Video detected. Compressing video file {}", originalFilename);
            info.setExtensionType("video");
            info.setProcessedFile(processVideoFile(tempFile, originalFilename));
            info.setFinalFilename("compressed_" + originalFilename);

            // Upload the video first
            String folderPath = getS3FolderPath(extension);
            String s3Key = folderPath + info.getFinalFilename();
            uploadToS3WithMetadata(info.getFinalFilename(), info.getProcessedFile());

            if(postCount.getVideoCount() < videoCount) {
                // Then check moderation
                info.setInappropriate(checkVideoModeration(s3Key));
            }

            // If inappropriate, delete the uploaded file
            if (info.isInappropriate()) {
                deleteExistingObject(s3Key);
            }
        } else if (validateImageVideo.isImage(extension)) {
            info.setExtensionType("image");
            info.setProcessedFile(tempFile);
            info.setFinalFilename(originalFilename);
            if(postCount.getImageCount() < imageCount){
                info.setInappropriate(checkImageModeration(info.getProcessedFile()));
            }
        } else {
            info.setProcessedFile(tempFile);
            info.setFinalFilename(originalFilename);
        }

        return info;
    }

    private File processVideoFile(File tempFile, String originalFilename) throws Exception {
        log.info("LOG:: Compressing video file {}", originalFilename);
        File processedFile = new File(System.getProperty("java.io.tmpdir"), "compressed_" + originalFilename);
        videoCompressionService.compressVideo(tempFile, processedFile);
        if (!processedFile.exists()) {
            throw new IOException("Video compression failed");
        }
        return processedFile;
    }

    private boolean checkImageModeration(File file) throws IOException {
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        // Convert to SdkBytes instead of ByteBuffer for AWS SDK v2
        SdkBytes sdkBytes = SdkBytes.fromByteArray(fileBytes);
        
        Image rekognitionImage = Image.builder()
                .bytes(sdkBytes)
                .build();

        DetectModerationLabelsRequest request = DetectModerationLabelsRequest.builder()
                .image(rekognitionImage)
                .minConfidence(minConfidence)
                .build();

        DetectModerationLabelsResponse result = rekognitionClient.detectModerationLabels(request);
        return result.moderationLabels().stream()
                .anyMatch(label -> INAPPROPRIATE_LABELS.contains(label.name()));
    }

    private boolean checkVideoModeration(String s3Key) throws Exception {
        StartContentModerationRequest request = StartContentModerationRequest.builder()
                .video(Video.builder()
                        .s3Object(S3Object.builder()
                                .bucket(bucketName)
                                .name(s3Key)
                                .build())
                        .build())
                .minConfidence(minConfidence)
                .build();

        StartContentModerationResponse startResponse = rekognitionClient.startContentModeration(request);
        String jobId = startResponse.jobId();
        log.info("LOG:: Started video moderation job {} for object {}", jobId, s3Key);

        GetContentModerationResponse result;
        do {
            Thread.sleep(500); // Wait half a second between checks
            result = rekognitionClient.getContentModeration(
                    GetContentModerationRequest.builder().jobId(jobId).build());
            log.debug("Video moderation job status: {}", result.jobStatus());
        } while (result.jobStatus() == VideoJobStatus.IN_PROGRESS);

        if (result.jobStatus() == VideoJobStatus.SUCCEEDED) {
            return result.moderationLabels().stream()
                    .anyMatch(detection -> INAPPROPRIATE_LABELS.contains(
                            detection.moderationLabel().name()));
        } else {
            log.error("Video moderation failed for object {}: {}", s3Key, result.statusMessage());
            throw new IOException("Video moderation failed: " + result.statusMessage());
        }
    }

    private String uploadProcessedFile(ProcessedFileInfo processedInfo, ImageType imageType) throws IOException {
        uploadToS3WithMetadata(processedInfo.getFinalFilename(), processedInfo.getProcessedFile());
        return generateImageUrl(processedInfo.getFinalFilename(), imageType);
    }

    private String getS3FolderPath(String extension) {
        if (validateImageVideo.isVideo(extension)) {
            return "video/";
        } else if (validateImageVideo.isImage(extension)) {
            return "images/";
        } else if (extension.equals("pdf")) {
            return "documents/";
        }
        return "others/";
    }

    private void uploadToS3WithMetadata(String filename, File file) throws IOException {
        String contentType = resolveContentType(filename);
        long contentLength = Files.size(file.toPath());

        // Get the appropriate folder path based on file extension
        String folderPath = getS3FolderPath(FilenameUtils.getExtension(filename).toLowerCase());
        String s3Key = folderPath + filename;

        // Create metadata map for AWS SDK v2
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Title", "File Upload - " + filename);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .contentLength(contentLength)
                .metadata(metadata)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromFile(file));
        log.info("LOG:: File uploaded successfully to S3: {}", s3Key);
    }

    private String generateImageUrl(String fileName, ImageType imageType) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        String folderPath = getS3FolderPath(extension);
        if(validateImageVideo.isImage(extension)){
            ImageProcessingResponseDTO imageProcessingResponseDTO = imageProcessingService.processImage(fileName);
            log.info("LOG:: imageProcessingResponseDTO {}", imageProcessingResponseDTO);
            if(imageProcessingResponseDTO != null && imageProcessingResponseDTO.getStatus().equals("success")){
                return switch (imageType) {
                    case SIGNATURE -> imageProcessingResponseDTO.getUrls().getLandscape();
                    case PROFILE_IMAGE, CLUB_LOGO, COVER_IMAGE, PROFILE_BANNER_IMAGE ->
                            imageProcessingResponseDTO.getUrls().getProfile();
                    default -> imageProcessingResponseDTO.getUrls().getStory();
                };
            }
        }
        return "https://" + cloudFrontUrl + "/" + folderPath + fileName;
    }

    private String resolveContentType(String filename) {
        String extension = FilenameUtils.getExtension(filename).toLowerCase();
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    private void cleanupFiles(File... files) {
        for (File file : files) {
            if (file != null && file.exists()) {
                try {
                    Files.delete(file.toPath());
                    log.debug("Temporary file deleted: {}", file.getName());
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", file.getName(), e);
                }
            }
        }
    }

    private CommonResponse buildSuccessResponse(String imageUrl) {
        ImageDto imageDto = ImageDto.builder()
                .imageUrl(imageUrl)
                .build();
        return CommonResponse.builder()
                .data(imageDto)
                .meta(new MetaData(false, CommonMessages.REQUEST_SUCCESS, 200, "Uploaded successfully"))
                .status(StatusType.STATUS_SUCCESS)
                .build();
    }

    private CommonResponse buildFailureResponse(String errorMessage) {
        return CommonResponse.builder()
                .data(errorMessage)
                .meta(new MetaData(true, CommonMessages.INTERNAL_SERVER_ERROR, 500, ""))
                .status(StatusType.STATUS_FAIL)
                .build();
    }

    private CommonResponse handleRequestError() {
        return CommonResponse.builder()
                .meta(new MetaData(true, CommonMessages.INAPPROPRIATE_CONTENT, 400,
                        "File contains inappropriate content"))
                .status(StatusType.STATUS_FAIL)
                .build();
    }

}