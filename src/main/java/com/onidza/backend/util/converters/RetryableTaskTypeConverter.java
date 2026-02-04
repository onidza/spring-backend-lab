package com.onidza.backend.util.converters;

import com.onidza.backend.model.dto.enums.RetryableTaskType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RetryableTaskTypeConverter implements AttributeConverter<RetryableTaskType, String> {

    @Override
    public String convertToDatabaseColumn(RetryableTaskType type) {
        if (type == null) return null;

        return type.getValue();
    }

    @Override
    public RetryableTaskType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        return RetryableTaskType.fromValue(dbData);
    }
}
