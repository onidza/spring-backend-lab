package com.onidza.backend.model.dto.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RetryableTaskStatus {
    RETRY("RETRY"),
    IN_PROGRESS("IN PROGRESS"),
    SUCCESS("SUCCESS");

    private final String value;

    public static RetryableTaskStatus fromValue(String value) {
        for (RetryableTaskStatus status : RetryableTaskStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown type: " + value);
    }
}
