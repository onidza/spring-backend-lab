--liquibase formatted sql

--changeset onidza:011-drop_old_retryable_task

DROP TABLE IF EXISTS retryable_task;