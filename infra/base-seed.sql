----------- Tune for fast bulk load (safe for test env) -------

SET client_min_messages = warning;
SET synchronous_commit = off;
SET statement_timeout = 0;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

--------------------------- CLEANUP ---------------------------

TRUNCATE TABLE
    client_coupons,
    orders,
    clients,
    coupons,
    profiles
    RESTART IDENTITY;

--------------------------- INSERTS (~10 000 000) -------------

-- 1) PROFILES (2,000,000)
INSERT INTO profiles (address, phone)
SELECT
    'Street ' || (gs % 100000)::text || ', House ' || (1 + (gs % 200))::text || ', Apt ' || (1 + (gs % 300))::text AS address,
    '+7' || lpad(((9000000000::bigint + (gs % 999999999))::text), 10, '0') AS phone
FROM generate_series(1, 2000000) AS gs;

-- 2) CLIENTS (2,000,000)  profile_id = id (1:1)
INSERT INTO clients (profile_id, registration_date, email, name)
SELECT
    gs AS profile_id,
    -- registration_date within last ~3 years
    (now() - ((gs % 1095) || ' days')::interval - ((gs % 86400) || ' seconds')::interval)::timestamp AS registration_date,
    -- email deterministic unique-ish
    ('user' || gs::text || '@example.com')::varchar(255) AS email,
    ('User ' || gs::text)::varchar(255) AS name
FROM generate_series(1, 2000000) AS gs;

-- 3) COUPONS (500,000)
INSERT INTO coupons (code, discount, expiration_date)
SELECT
    -- code like "CPN-<hash>"
    ('CPN-' || substr(encode(digest(gs::text, 'sha256'), 'hex'), 1, 12))::varchar(255) AS code,
    -- discount 1..70 (%)
    (1 + (gs % 70))::real AS discount,
    -- expiration_date: some expired, some future
    (now() + (((gs % 900) - 300) || ' days')::interval)::timestamp AS expiration_date
FROM generate_series(1, 500000) AS gs;

-- 4) ORDERS (6,500,000)
-- client_id is distributed across 2m clients
-- order_date within last 365 days
-- status is one of common values
INSERT INTO orders (client_id, order_date, status, total_amount)
SELECT
    (1 + (gs % 2000000))::bigint AS client_id,
    (now() - ((gs % 365) || ' days')::interval - ((gs % 86400) || ' seconds')::interval)::timestamp AS order_date,
    (CASE
         WHEN (gs % 100) < 15 THEN 'NEW'
         WHEN (gs % 100) < 90 THEN 'PAID'
         ELSE 'CANCELLED'
        END)::varchar(255) AS status,
    -- total_amount: 10.00 ... 5000.00
    round((10 + (gs % 4991) + ((gs % 100)::numeric / 100))::numeric, 2) AS total_amount
FROM generate_series(1, 6500000) AS gs;

-- 5) CLIENT_COUPONS (1,000,000)
-- Ensure (client_id, coupon_id) unique due to PK
-- Map i -> client_id, coupon_id in a way that doesn't collide
INSERT INTO client_coupons (client_id, coupon_id)
SELECT
    (1 + (gs % 2000000))::bigint AS client_id,
    (1 + ((gs * 37) % 500000))::bigint AS coupon_id
FROM generate_series(1, 1000000) AS gs
ON CONFLICT DO NOTHING;

-- ----------------------- INSERT TEST (~100) ------------------
--
-- -- 1) PROFILES (100)
-- INSERT INTO profiles (address, phone)
-- SELECT
--     'Street ' || (gs % 100)::text || ', House ' || (1 + (gs % 20))::text || ', Apt ' || (1 + (gs % 30))::text AS address,
--     '+7' || lpad(((9000000000::bigint + gs)::text), 10, '0') AS phone
-- FROM generate_series(1, 100) AS gs;
--
-- -- 2) CLIENTS (100)  profile_id = id (1:1)
-- INSERT INTO clients (profile_id, registration_date, email, name)
-- SELECT
--     gs AS profile_id,
--     (now() - ((gs % 30) || ' days')::interval - ((gs % 86400) || ' seconds')::interval)::timestamp AS registration_date,
--     ('user' || gs::text || '@example.com')::varchar(255) AS email,
--     ('User ' || gs::text)::varchar(255) AS name
-- FROM generate_series(1, 100) AS gs;
--
-- -- 3) COUPONS (20)
-- INSERT INTO coupons (code, discount, expiration_date)
-- SELECT
--     ('CPN-' || substr(encode(digest(gs::text, 'sha256'), 'hex'), 1, 8))::varchar(255) AS code,
--     (5 + (gs % 30))::real AS discount,
--     (now() + (((gs % 60) - 30) || ' days')::interval)::timestamp AS expiration_date
-- FROM generate_series(1, 20) AS gs;
--
-- -- 4) ORDERS (300)
-- INSERT INTO orders (client_id, order_date, status, total_amount)
-- SELECT
--     (1 + (gs % 100))::bigint AS client_id,
--     (now() - ((gs % 14) || ' days')::interval - ((gs % 86400) || ' seconds')::interval)::timestamp AS order_date,
--     (CASE
--          WHEN (gs % 100) < 15 THEN 'NEW'
--          WHEN (gs % 100) < 90 THEN 'PAID'
--          ELSE 'CANCELLED'
--         END)::varchar(255) AS status,
--     round((10 + (gs % 500) + ((gs % 100)::numeric / 100))::numeric, 2) AS total_amount
-- FROM generate_series(1, 300) AS gs;
--
-- -- 5) CLIENT_COUPONS (200)
-- INSERT INTO client_coupons (client_id, coupon_id)
-- SELECT
--     (1 + (gs % 100))::bigint AS client_id,
--     (1 + ((gs * 7) % 20))::bigint AS coupon_id
-- FROM generate_series(1, 200) AS gs
-- ON CONFLICT DO NOTHING;

----------------------- POST-LOAD ANALYZE ---------------------

ANALYZE profiles;
ANALYZE clients;
ANALYZE coupons;
ANALYZE orders;
ANALYZE client_coupons;

SELECT 'profiles', count(*) FROM profiles
UNION ALL SELECT 'clients', count(*) FROM clients
UNION ALL SELECT 'coupons', count(*) FROM coupons
UNION ALL SELECT 'orders', count(*) FROM orders
UNION ALL SELECT 'client_coupons', count(*) FROM client_coupons;