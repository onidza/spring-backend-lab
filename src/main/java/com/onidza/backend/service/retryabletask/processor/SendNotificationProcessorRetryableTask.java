package com.onidza.backend.service.retryabletask.processor;

import com.onidza.backend.model.entity.RetryableTask;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SendNotificationProcessorRetryableTask implements RetryableTaskProcessor {

    @Override
    public void processRetryableTask(List<RetryableTask> retryableTasks) {

    }
}
