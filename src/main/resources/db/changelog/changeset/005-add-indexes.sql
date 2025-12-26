--liquibase formatted sql

--changeset onidza:005-add-indexes

CREATE INDEX IF NOT EXISTS idx_client_coupon_client_id ON client_coupons(client_id);