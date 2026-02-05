--liquibase formatted sql

--changeset onidza:010-create-retryable-task-table-fixed

CREATE TABLE IF NOT EXISTS retryable_task
(
    uuid       UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    INTEGER                  NOT NULL,

    payload    JSONB                    NOT NULL,
    type       VARCHAR(100)             NOT NULL,
    status     VARCHAR(50)              NOT NULL,
    retryTime  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_retryable_task_status_retry_time
    ON retryable_task (type, status, retryTime);