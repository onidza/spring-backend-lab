package com.onidza.backend.util.converters;

import com.onidza.backend.model.dto.enums.RetryableTaskStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RetryableTaskStatusConverter implements AttributeConverter<RetryableTaskStatus, String> {


    @Override
    public String convertToDatabaseColumn(RetryableTaskStatus status) {
        if (status == null) return null;

        return status.getValue();
    }

    @Override
    public RetryableTaskStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        return RetryableTaskStatus.fromValue(dbData);
    }
}
