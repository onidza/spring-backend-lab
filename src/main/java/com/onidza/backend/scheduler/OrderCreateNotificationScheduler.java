package com.onidza.backend.scheduler;

import com.onidza.backend.kafka.producer.PublisherOrderNotification;
import com.onidza.backend.model.entity.RetryableTask;
import com.onidza.backend.model.enums.RetryableTaskStatus;
import com.onidza.backend.service.retryable.RetryableTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreateNotificationScheduler {

    private final PublisherOrderNotification publisherOrderNotification;
    private final RetryableTaskService retryableTaskService;


    @Scheduled(cron = "0 */1 * * * *")
    public void executeRetryableTasks() {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(Duration.ofMinutes(5));

        List<RetryableTask> tasks =
                retryableTaskService.claimBatchForProcessing(
                        RetryableTaskStatus.RETRY,
                        RetryableTaskStatus.IN_PROGRESS,
                        now,
                        staleBefore
                );

        if (tasks.isEmpty()) return;

        log.info("Found retryable tasks batch = {} size for processing",
                tasks.size());

        tasks.forEach(this::publishTask);
    }

    private void publishTask(RetryableTask task) {
        UUID taskId = task.getUuid();

        publisherOrderNotification
                .sendNotificationInTopic(taskId, task.getPayload())
                .whenComplete((result, ex)
                        -> handleSendResult(taskId, ex));
    }

    private void handleSendResult(UUID taskId, Throwable ex) {
        try {
            if (ex == null) {
                retryableTaskService.markSuccess(taskId);
                log.info("RetryableTask = {} was sent successfully", taskId);
            } else {
                retryableTaskService.markRetry(taskId, ex);
                log.warn("RetryableTask = {} failed to send", taskId, ex);
            }
        } catch (Exception callbackException) {
            log.error("Failed to update RetryableTask status, uuid = {}", taskId, callbackException);
        }
    }
}
