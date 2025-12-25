--liquibase formatted sql

--changeset onidza:002-init

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS address character varying(255);

UPDATE profiles SET address = ' ' WHERE address IS NULL;

ALTER TABLE profiles ALTER COLUMN address SET NOT NULL;
















