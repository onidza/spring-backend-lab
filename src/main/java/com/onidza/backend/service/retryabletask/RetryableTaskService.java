package com.onidza.backend.service.retryabletask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.model.dto.enums.RetryableTaskType;
import com.onidza.backend.model.entity.Order;
import com.onidza.backend.model.entity.RetryableTask;
import com.onidza.backend.model.mapper.RetryableTaskMapper;
import com.onidza.backend.repository.RetryableTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetryableTaskService {

    private final RetryableTaskRepository retryableTaskRepository;
    private final RetryableTaskMapper retryableTaskMapper;
    private final ObjectMapper objectMapper;

    public RetryableTask save(Order order, RetryableTaskType type) {
        RetryableTask retryableTask = retryableTaskMapper.toRetryableTask(
                order,
                type,
                objectMapper
        );

        return retryableTaskRepository.save(retryableTask);
    }
}
