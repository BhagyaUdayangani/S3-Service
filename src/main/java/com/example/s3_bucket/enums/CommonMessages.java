package com.example.s3_bucket.enums;

import lombok.Getter;

/**
 * Enum containing common API response messages.
 * Each enum constant represents a standardized message used across the application.
 */
@Getter
public enum CommonMessages {
    REQUEST_SUCCESS("Success Request", 200),
    BAD_CREDENTIALS("Bad Credentials", 401),
    FORBIDDEN_ACCESS("Forbidden Access", 403),
    REQUEST_FAIL("Failed Request", 400),
    REQUEST_NO_LONGER_USED("Resource No Longer Used", 410),
    INTERNAL_SERVER_ERROR("Internal Server Error", 500),
    REQUEST_CONFLICT("Conflict Request", 409),
    INAPPROPRIATE_CONTENT("Content Not Allowed", 422);

    private final String message;
    private final int defaultStatusCode;

    CommonMessages(String message, int defaultStatusCode) {
        this.message = message;
        this.defaultStatusCode = defaultStatusCode;
    }

    /**
     * Returns the message in a format suitable for logging or display.
     *
     * @return formatted message string
     */
    @Override
    public String toString() {
        return this.message;
    }
}