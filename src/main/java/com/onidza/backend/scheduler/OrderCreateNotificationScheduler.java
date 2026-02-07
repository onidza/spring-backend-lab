package com.onidza.backend.scheduler;

import com.onidza.backend.kafka.PublisherOrderNotification;
import com.onidza.backend.model.dto.enums.RetryableTaskStatus;
import com.onidza.backend.model.entity.RetryableTask;
import com.onidza.backend.service.retryabletask.RetryableTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreateNotificationScheduler {

    private final PublisherOrderNotification publisherOrderNotification;
    private final RetryableTaskService retryableTaskService;


    @Scheduled(cron = "0 */1 * * * *")
    public void executeRetryableTasks() {
        List<RetryableTask> tasks =
                retryableTaskService.getBatchForProcessing(
                        RetryableTaskStatus.RETRY,
                        RetryableTaskStatus.IN_PROGRESS
                );

        if (tasks.isEmpty()) {
            log.debug("No retryable tasks found");
            return;
        }

        log.info("Found {} retryable tasks for processing", tasks.size());

        tasks.forEach(t ->
                publisherOrderNotification.sendNotificationInTopic(t.getUuid(), t.getPayload()));
    }
}
