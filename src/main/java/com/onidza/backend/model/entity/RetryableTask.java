package com.onidza.backend.model.entity;

import com.onidza.backend.model.dto.enums.RetryableTaskStatus;
import com.onidza.backend.model.dto.enums.RetryableTaskType;
import com.onidza.backend.util.converters.RetryableTaskStatusConverter;
import com.onidza.backend.util.converters.RetryableTaskTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

@Generated
@Entity
@Setter
@Getter
@Table(name = "retryable_task")
public class RetryableTask extends BaseKafkaEntity {

    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String payload;

    @Convert(converter = RetryableTaskTypeConverter.class)
    RetryableTaskType type;

    @Convert(converter = RetryableTaskStatusConverter.class)
    RetryableTaskStatus status;

    private Instant retryTime;
}
