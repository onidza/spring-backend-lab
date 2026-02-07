package com.onidza.backend.repository;

import com.onidza.backend.model.dto.enums.RetryableTaskStatus;
import com.onidza.backend.model.entity.RetryableTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetryableTaskRepository extends JpaRepository<RetryableTask, UUID> {
    Optional<RetryableTask> findByUuid(UUID uuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t
            FROM RetryableTask t
            WHERE t.retryTime <= :now
              AND (t.status = :retryStatus OR t.status = :inProgressStatus)
            ORDER BY t.retryTime
            """)
    List<RetryableTask> findAllForProcessing(
            @Param("retryStatus") RetryableTaskStatus retryStatus,
            @Param("inProgressStatus") RetryableTaskStatus inProgressStatus,
            @Param("now") Instant now,
            Pageable pageable
    );
}
