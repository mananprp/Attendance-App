-- MySQL Database Schema for Attendance App Backend
-- Target Engine: MySQL 8.0+ / InnoDB / utf8mb4

CREATE DATABASE IF NOT EXISTS `attendance_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `attendance_db`;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `status` ENUM('ACTIVE', 'SUSPENDED', 'DEACTIVATED') NOT NULL DEFAULT 'ACTIVE',
  `photo_change_used` BOOLEAN NOT NULL DEFAULT FALSE,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. User Photos Table (strictly enforces 1 or 2 versions per user)
CREATE TABLE IF NOT EXISTS `user_photos` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(64) NOT NULL,
  `version` TINYINT UNSIGNED NOT NULL COMMENT '1 = onboarding, 2 = one-time replacement',
  `image_path` VARCHAR(512) NOT NULL,
  `hash` VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash of image file',
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
  `uploaded_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_version` (`user_id`, `version`),
  CONSTRAINT `chk_user_photo_version` CHECK (`version` IN (1, 2)),
  CONSTRAINT `fk_user_photos_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Office Configuration Table (office point, radius, sync windows, timezone, match threshold)
CREATE TABLE IF NOT EXISTS `office_config` (
  `office_id` VARCHAR(64) NOT NULL DEFAULT 'DEFAULT_OFFICE',
  `name` VARCHAR(128) NOT NULL DEFAULT 'Headquarters',
  `latitude` DECIMAL(10, 7) NOT NULL,
  `longitude` DECIMAL(10, 7) NOT NULL,
  `radius_m` INT UNSIGNED NOT NULL DEFAULT 100,
  `timezone` VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata',
  `sync_windows` JSON NOT NULL COMMENT 'Array of HH:mm strings, e.g. ["16:00", "00:00"]',
  `sync_jitter_minutes` INT UNSIGNED NOT NULL DEFAULT 30,
  `match_threshold` DECIMAL(5, 4) NOT NULL DEFAULT 0.5500,
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`office_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Sync Batches Table (audit of batch uploads)
CREATE TABLE IF NOT EXISTS `sync_batches` (
  `batch_id` CHAR(36) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `records_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `received_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `status` ENUM('SUCCESS', 'PARTIAL', 'FAILED') NOT NULL DEFAULT 'SUCCESS',
  PRIMARY KEY (`batch_id`),
  KEY `idx_sync_batches_user` (`user_id`),
  CONSTRAINT `fk_sync_batches_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Attendance Punches Table (immutable log of all verified punches)
CREATE TABLE IF NOT EXISTS `attendance_punches` (
  `punch_uuid` CHAR(36) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `date_local` DATE NOT NULL COMMENT 'Date in office local timezone (YYYY-MM-DD)',
  `type` ENUM('IN', 'OUT') NOT NULL,
  `punch_time_utc` DATETIME(3) NOT NULL,
  `time_source` ENUM('NTP', 'DEVICE') NOT NULL DEFAULT 'NTP',
  `time_unverified` BOOLEAN NOT NULL DEFAULT FALSE,
  `latitude` DECIMAL(10, 7) NOT NULL,
  `longitude` DECIMAL(10, 7) NOT NULL,
  `distance_m` DECIMAL(8, 2) NOT NULL,
  `match_score` DECIMAL(5, 4) NOT NULL,
  `image_path` VARCHAR(512) NULL,
  `sync_batch_id` CHAR(36) NULL,
  `received_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`punch_uuid`),
  KEY `idx_punches_user_date` (`user_id`, `date_local`),
  KEY `idx_punches_sync_batch` (`sync_batch_id`),
  CONSTRAINT `fk_punches_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_punches_batch` FOREIGN KEY (`sync_batch_id`) REFERENCES `sync_batches` (`batch_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Attendance Days Summary Table (materialized daily summary for calendar & queries)
CREATE TABLE IF NOT EXISTS `attendance_days` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(64) NOT NULL,
  `date` DATE NOT NULL COMMENT 'Date in office local timezone (YYYY-MM-DD)',
  `in_time` DATETIME(3) NULL,
  `out_time` DATETIME(3) NULL,
  `status` ENUM('COMPLETE', 'MISSED_PUNCH', 'NO_RECORD') NOT NULL DEFAULT 'NO_RECORD',
  `time_unverified` BOOLEAN NOT NULL DEFAULT FALSE,
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `date`),
  KEY `idx_attendance_days_user_month` (`user_id`, `date`),
  CONSTRAINT `fk_attendance_days_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default Seed Data for Office Config
INSERT INTO `office_config` (`office_id`, `name`, `latitude`, `longitude`, `radius_m`, `timezone`, `sync_windows`, `sync_jitter_minutes`, `match_threshold`)
VALUES ('DEFAULT_OFFICE', 'Headquarters', 28.6139390, 77.2090210, 100, 'Asia/Kolkata', '["16:00", "00:00"]', 30, 0.5500)
ON DUPLICATE KEY UPDATE `office_id`=`office_id`;
