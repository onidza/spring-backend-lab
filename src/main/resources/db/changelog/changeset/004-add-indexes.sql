--liquibase formatted sql

--changeset onidza:004-add-indexes-for-orders

CREATE INDEX IF NOT EXISTS idx_orders_status_order_date ON orders(status, order_date);