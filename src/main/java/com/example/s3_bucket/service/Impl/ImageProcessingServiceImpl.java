package com.example.s3_bucket.service.Impl;

import com.example.s3_bucket.dto.ImageProcessingRequest;
import com.example.s3_bucket.dto.ImageProcessingResponseDTO;
import com.example.s3_bucket.service.ImageProcessingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {

    private final RestTemplate restTemplate;

    @Value("${api.class.s3.image.bucket-name}")
    private String bucketName;

    @Value("${cloud-front.url}")
    private String cloudFrontUrl;

    @Value("${aws.image.processing.url}")
    private String imageProcessingUrl;

    @Value("${aws.image.processing.uri}")
    private String imageProcessingUri;

    @Value("${aws.image.processing.auth-token}")
    private String authToken;

    // Constructor for production use with default RestTemplate
    public ImageProcessingServiceImpl() {
        this.restTemplate = new RestTemplate();
        // Add String converter first to handle text/plain responses
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    // Constructor for testing with RestTemplate mock
    public ImageProcessingServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ImageProcessingResponseDTO processImage(String imageKey) {
        try {
            String url = imageProcessingUrl + imageProcessingUri;
            ImageProcessingRequest request = new ImageProcessingRequest(imageKey, bucketName, cloudFrontUrl);

//            String response = restTemplate.postForObject(url, request, String.class);

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authToken);

            // Create an HTTP entity with headers and body
            HttpEntity<ImageProcessingRequest> entity = new HttpEntity<>(request, headers);

            // Execute request with headers
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            String response = responseEntity.getBody();

            // Then parse it to your DTO
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(response, ImageProcessingResponseDTO.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Image processing service error: Status={}, Body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            // Return default response to continue processing
            return createFallbackResponse();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse response: {}", e.getMessage());

            // Return default response to continue processing
            return createFallbackResponse();
        }
    }

    private ImageProcessingResponseDTO createFallbackResponse() {
        // Create a fallback response with default values or placeholder URLs
        ImageProcessingResponseDTO fallbackResponse = new ImageProcessingResponseDTO();
        fallbackResponse.setStatus("error");

        return fallbackResponse;
    }
}