--liquibase formatted sql

--changeset onidza:003-add-orders-client-id-index

CREATE INDEX IF NOT EXISTS idx_orders_client_id ON orders(client_id);