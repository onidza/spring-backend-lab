package com.onidza.backend.service.retryabletask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.model.dto.enums.RetryableTaskStatus;
import com.onidza.backend.model.dto.enums.RetryableTaskType;
import com.onidza.backend.model.dto.order.OrderCreateEvent;
import com.onidza.backend.model.entity.RetryableTask;
import com.onidza.backend.model.mapper.RetryableTaskMapper;
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
    private final RetryableTaskMapper retryableTaskMapper;
    private final ObjectMapper objectMapper;

    @Value("${retryable_task.timeoutInSeconds}")
    private Integer retryTime;

    @Value("${retryable_task.limit}")
    private Integer limit;

    @Transactional
    public void createRetryableTask(OrderCreateEvent event, RetryableTaskType type) {
        RetryableTask retryableTask = retryableTaskMapper.toRetryableTask(
                event,
                type,
                objectMapper
        );

        log.info("RetryableTask with uuid: {} was saved in db.", retryableTask.getUuid());
        retryableTaskRepository.save(retryableTask);
    }

    @Transactional
    public List<RetryableTask> getBatchForProcessing(
            RetryableTaskStatus retryStatus,
            RetryableTaskStatus inProgressStatus
    ) {
        var tasks = retryableTaskRepository.findAllForProcessing(
                retryStatus,
                inProgressStatus,
                Instant.now(),
                PageRequest.of(0, limit)
        );

        Instant newRetryTime = Instant.now().plus(Duration.ofSeconds(retryTime));

        tasks.forEach(t -> {
            t.setRetryTime(newRetryTime);
            t.setStatus(RetryableTaskStatus.IN_PROGRESS);
            log.info("RetryableTask {} was delayed on {}", t.getUuid(), newRetryTime);
        });

        return tasks;
    }

    @Transactional
    public void markSent(UUID uuid) {
        RetryableTask existingTask = retryableTaskRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "RetryableTask not found, uuid=" + uuid
                ));

        existingTask.setStatus(RetryableTaskStatus.SUCCESS);
        log.info("RetryableTask {} status updated to SUCCESS", uuid);
    }
}
