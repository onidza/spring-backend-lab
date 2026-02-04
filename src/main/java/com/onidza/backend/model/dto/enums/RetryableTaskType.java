package com.onidza.backend.model.dto.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RetryableTaskType {
    SEND_CREATE_NOTIFICATION_REQUEST("SEND CREATE NOTIFICATION REQUEST"),
    SOME_OTHER_TYPE("TEST");

    private final String value;

    public static RetryableTaskType fromValue(String value) {
        for (RetryableTaskType type : RetryableTaskType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown type: " + value);
    }
}
