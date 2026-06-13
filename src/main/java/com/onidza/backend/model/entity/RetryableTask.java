package com.onidza.backend.model.entity;

import com.onidza.backend.model.enums.RetryableTaskStatus;
import com.onidza.backend.model.enums.RetryableTaskType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

@Entity
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "retryable_task")
public class RetryableTask extends BaseKafkaEntity {

    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String payload;

    @Enumerated(value = EnumType.STRING)
    private RetryableTaskType type;

    @Enumerated(value = EnumType.STRING)
    private RetryableTaskStatus status;

    private Instant retryTime;

    private RetryableTask(String payload, RetryableTaskType type) {
        this.payload = payload;
        this.type = type;
        this.status = RetryableTaskStatus.RETRY;
        this.retryTime = Instant.now();
    }

    public static RetryableTask retry(String payload, RetryableTaskType type) {
        return new RetryableTask(payload, type);
    }
}
