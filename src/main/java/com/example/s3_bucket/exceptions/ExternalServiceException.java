package com.example.s3_bucket.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when external service calls fail.
 * Provides detailed information about the failure, including HTTP status, service name,
 * and additional context about the error.
 */
@Getter
public class ExternalServiceException extends RuntimeException {
    private final HttpStatus status;
    private final String serviceName;
    private final String errorCode;

    /**
     * Creates a new external service exception with a message.
     *
     * @param message the error message
     */
    public ExternalServiceException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.serviceName = "UNKNOWN";
        this.errorCode = "EXTERNAL_SERVICE_ERROR";
    }

}