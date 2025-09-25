package com.example.s3_bucket.service.Impl;

import com.example.s3_bucket.exceptions.ExternalServiceException;
import com.example.s3_bucket.service.UserService;
import com.example.s3_bucket.annotation.TrackExecutionTime;
import com.spordee.user.dto.request.PostCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Implementation of UserService for managing user-related operations.
 * Handles communication with external user service for post-count information.
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final WebClient webClient;
    @Value("${service.method.get.user}") private String userUrl;

    public UserServiceImpl(@Qualifier("userWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Retrieves the post-count information for a user.
     *
     * @param authUserId the ID of the authenticated user
     * @param token the authentication token
     * @return PostCount object containing the user's post-statistics
     */

    @TrackExecutionTime
    @Override
    public PostCount getPostCount(String authUserId, String token) {
        if (authUserId == null || token == null) {
            log.warn("Invalid input parameters: authUserId or token is null");
            return createDefaultPostCount(authUserId);
        }

        return callExternalServiceWithGet(createPostCountRequest(authUserId), token)
                .doOnSuccess(count -> log.info("Successfully retrieved post count for user: {}", authUserId))
                .doOnError(error -> log.error("Error retrieving post count for user: {}", authUserId, error))
                .onErrorReturn(createDefaultPostCount(authUserId))
                .block();
    }

    /**
     * Creates a default PostCount object with zero counts.
     */
    private PostCount createDefaultPostCount(String authUserId) {
        return PostCount.builder()
                .count(0L)
                .videoCount(0L)
                .authUserId(authUserId)
                .imageCount(0L)
                .build();
    }

    /**
     * Creates a PostCount request object.
     */
    private PostCount createPostCountRequest(String authUserId) {
        return PostCount.builder()
                .authUserId(authUserId)
                .build();
    }

    /**
     * Makes an HTTP call to the external service to retrieve post-count information.
     */
    private Mono<PostCount> callExternalServiceWithGet(PostCount requestBody, String token) {
        return webClient
                .post()
                .uri(userUrl)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ExternalServiceException(
                                "External service error with status: " + response.statusCode()))
                )
                .bodyToMono(new ParameterizedTypeReference<PostCount>() {})
                .doOnSuccess(response -> log.debug("External service call successful"))
                .doOnError(error -> log.error("External service call failed", error));
    }


}