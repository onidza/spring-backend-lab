package com.onidza.backend.repository;

import com.onidza.backend.model.entity.RetryableTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RetryableTaskRepository extends JpaRepository<RetryableTask, UUID> {
}
