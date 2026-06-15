package com.onidza.backend.service.retryable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.model.dto.kafka.OrderCreateEvent;
import com.onidza.backend.model.entity.RetryableTask;
import com.onidza.backend.model.enums.RetryableTaskStatus;
import com.onidza.backend.model.enums.RetryableTaskType;
import com.onidza.backend.repository.RetryableTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryableTaskService {

    private final RetryableTaskRepository retryableTaskRepository;
    private final ObjectMapper objectMapper;

    @Value("${retryable_task.delay}")
    private Integer delay;

    @Value("${retryable_task.batchLimit}")
    private Integer batchLimit;

    @Transactional
    public void createRetryableTask(OrderCreateEvent event, RetryableTaskType type) {
        RetryableTask retryableTask = RetryableTask.retry(toJson(event), type);
        log.info("RetryableTask = {} was created", retryableTask);

        retryableTaskRepository.save(retryableTask);
    }

    @Transactional
    public List<RetryableTask> claimBatchForProcessing(
            RetryableTaskStatus retryStatus,
            RetryableTaskStatus inProgressStatus,
            Instant now,
            Instant staleBefore
    ) {
        var tasks = retryableTaskRepository.findAllForProcessing(
                retryStatus,
                inProgressStatus,
                now,
                staleBefore,
                PageRequest.of(0, batchLimit)
        );

        Instant newRetryTime = Instant.now().plus(Duration.ofSeconds(delay));

        tasks.forEach(t -> {
            t.setRetryTime(newRetryTime);
            t.setStatus(RetryableTaskStatus.IN_PROGRESS);

            log.info("RetryableTask = {} was claimed for processing until = {}",
                    t.getUuid(), newRetryTime);
        });

        return tasks;
    }

    @Transactional
    public void markSuccess(UUID uuid) {
        RetryableTask existingTask = retryableTaskRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "RetryableTask not found, uuid = " + uuid
                ));

        existingTask.setStatus(RetryableTaskStatus.SUCCESS);

        log.info("RetryableTask = {}, status updated to SUCCESS", uuid);
    }

    @Transactional
    public void markRetry(UUID uuid, Throwable ex) {
        RetryableTask existingTask = retryableTaskRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "RetryableTask not found, uuid = " + uuid
                ));

        Instant nextRetryTime = Instant.now().plus(Duration.ofSeconds(delay));

        existingTask.setStatus(RetryableTaskStatus.RETRY);
        existingTask.setRetryTime(nextRetryTime);

        log.warn("RetryableTask = {}, status returned to RETRY, nextRetryTime = {}",
                uuid, nextRetryTime, ex);
    }

    private String toJson(OrderCreateEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize OrderCreateEvent: event = {}", event, e);
            throw new IllegalStateException("Failed to serialize OrderCreateEvent", e);
        }
    }
}
