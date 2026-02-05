package com.onidza.backend.service.retryabletask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.model.dto.enums.RetryableTaskType;
import com.onidza.backend.model.dto.order.OrderCreateEvent;
import com.onidza.backend.model.entity.RetryableTask;
import com.onidza.backend.model.mapper.RetryableTaskMapper;
import com.onidza.backend.repository.RetryableTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryableTaskService {

    private final RetryableTaskRepository retryableTaskRepository;
    private final RetryableTaskMapper retryableTaskMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public RetryableTask createRetryableTask(OrderCreateEvent event, RetryableTaskType type) {
        RetryableTask retryableTask = retryableTaskMapper.toRetryableTask(
                event,
                type,
                objectMapper
        );

        log.info("RetryableTask with uuid: {} was saved in db.", retryableTask.getUuid());
        return retryableTaskRepository.save(retryableTask);
    }


}
