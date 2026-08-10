-- Creates one empty schema per Cake Delight microservice.
-- Prefixed with cake_delight_ so these never collide with unrelated schemas
-- on a shared MySQL/MariaDB instance (see docs/audits for context).
CREATE DATABASE IF NOT EXISTS cake_delight_auth_db         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cake_delight_catalog_db      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cake_delight_order_db        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cake_delight_rating_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cake_delight_notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
