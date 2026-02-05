package com.onidza.backend.service.retryabletask.processor;

import com.onidza.backend.model.entity.RetryableTask;

import java.util.List;

public interface RetryableTaskProcessor {

    void processRetryableTask(List<RetryableTask> retryableTasks);
}
