package com.example.s3_bucket.enums;

import lombok.Getter;

/**
 * Enum representing the status of operations in the system.
 * Used to indicate the outcome of various operations and processes.
 */
@Getter
public enum StatusType {
    STATUS_SUCCESS("Success", true),
    STATUS_FAIL("Failed", false),
    STATUS_PENDING("Pending", false);

    private final String displayValue;
    private final boolean successful;

    /**
     * Constructor for StatusType enum.
     *
     * @param displayValue The human-readable status message
     * @param successful  Whether this status represents a successful operation
     */
    StatusType(String displayValue, boolean successful) {
        this.displayValue = displayValue;
        this.successful = successful;
    }

    /**
     * Returns the display value of the status.
     *
     * @return formatted status string
     */
    @Override
    public String toString() {
        return this.displayValue;
    }

}