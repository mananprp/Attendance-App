-- ==============================================================================
-- Migration: Field Tracking Tables (Sessions, Points, Stops)
-- Target: Microsoft SQL Server
-- Description: Creates tables for recording supervisor & employee field tracking paths.
-- Safe to run multiple times (idempotent).
-- ==============================================================================

-- 1. Create [tracking_sessions] table
IF NOT EXISTS (
    SELECT 1 FROM sys.tables WHERE name = 'tracking_sessions' AND schema_id = SCHEMA_ID('dbo')
)
BEGIN
    CREATE TABLE [dbo].[tracking_sessions] (
        [id] VARCHAR(36) NOT NULL,
        [user_id] VARCHAR(64) NOT NULL,
        [start_time_utc] DATETIME NOT NULL,
        [end_time_utc] DATETIME NULL,
        [total_distance_m] DECIMAL(10, 2) NOT NULL DEFAULT 0,
        [status] VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
        [stops_count] INT NOT NULL DEFAULT 0,
        [created_at] DATETIME NOT NULL DEFAULT GETUTCDATE(),
        CONSTRAINT [PK_tracking_sessions] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_tracking_sessions_users] FOREIGN KEY ([user_id]) 
            REFERENCES [dbo].[users] ([user_id]) ON DELETE CASCADE
    );

    CREATE NONCLUSTERED INDEX [IX_tracking_sessions_user_start] 
        ON [dbo].[tracking_sessions] ([user_id] ASC, [start_time_utc] ASC);
END;

-- 2. Create [tracking_points] table
IF NOT EXISTS (
    SELECT 1 FROM sys.tables WHERE name = 'tracking_points' AND schema_id = SCHEMA_ID('dbo')
)
BEGIN
    CREATE TABLE [dbo].[tracking_points] (
        [id] VARCHAR(36) NOT NULL,
        [session_id] VARCHAR(36) NOT NULL,
        [latitude] DECIMAL(10, 7) NOT NULL,
        [longitude] DECIMAL(10, 7) NOT NULL,
        [accuracy_m] REAL NULL,
        [recorded_at_utc] DATETIME NOT NULL,
        [is_paused_gap] BIT NOT NULL DEFAULT 0,
        CONSTRAINT [PK_tracking_points] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_tracking_points_session] FOREIGN KEY ([session_id]) 
            REFERENCES [dbo].[tracking_sessions] ([id]) ON DELETE CASCADE
    );

    CREATE NONCLUSTERED INDEX [IX_tracking_points_session_time] 
        ON [dbo].[tracking_points] ([session_id] ASC, [recorded_at_utc] ASC);
END;

-- 3. Create [tracking_stops] table
IF NOT EXISTS (
    SELECT 1 FROM sys.tables WHERE name = 'tracking_stops' AND schema_id = SCHEMA_ID('dbo')
)
BEGIN
    CREATE TABLE [dbo].[tracking_stops] (
        [id] VARCHAR(36) NOT NULL,
        [session_id] VARCHAR(36) NOT NULL,
        [latitude] DECIMAL(10, 7) NOT NULL,
        [longitude] DECIMAL(10, 7) NOT NULL,
        [stop_name] VARCHAR(128) NULL,
        [arrived_at_utc] DATETIME NOT NULL,
        CONSTRAINT [PK_tracking_stops] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_tracking_stops_session] FOREIGN KEY ([session_id]) 
            REFERENCES [dbo].[tracking_sessions] ([id]) ON DELETE CASCADE
    );

    CREATE NONCLUSTERED INDEX [IX_tracking_stops_session_time] 
        ON [dbo].[tracking_stops] ([session_id] ASC, [arrived_at_utc] ASC);
END;

-- ==============================================================================
-- MySQL equivalent (for reference):
--
-- CREATE TABLE IF NOT EXISTS `tracking_sessions` (
--     `id` VARCHAR(36) NOT NULL PRIMARY KEY,
--     `user_id` VARCHAR(64) NOT NULL,
--     `start_time_utc` DATETIME NOT NULL,
--     `end_time_utc` DATETIME NULL,
--     `total_distance_m` DECIMAL(10, 2) NOT NULL DEFAULT 0,
--     `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
--     `stops_count` INT NOT NULL DEFAULT 0,
--     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     INDEX `IX_tracking_sessions_user_start` (`user_id`, `start_time_utc`),
--     FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
-- ) ENGINE=InnoDB;
--
-- CREATE TABLE IF NOT EXISTS `tracking_points` (
--     `id` VARCHAR(36) NOT NULL PRIMARY KEY,
--     `session_id` VARCHAR(36) NOT NULL,
--     `latitude` DECIMAL(10, 7) NOT NULL,
--     `longitude` DECIMAL(10, 7) NOT NULL,
--     `accuracy_m` FLOAT NULL,
--     `recorded_at_utc` DATETIME NOT NULL,
--     `is_paused_gap` TINYINT(1) NOT NULL DEFAULT 0,
--     INDEX `IX_tracking_points_session_time` (`session_id`, `recorded_at_utc`),
--     FOREIGN KEY (`session_id`) REFERENCES `tracking_sessions` (`id`) ON DELETE CASCADE
-- ) ENGINE=InnoDB;
--
-- CREATE TABLE IF NOT EXISTS `tracking_stops` (
--     `id` VARCHAR(36) NOT NULL PRIMARY KEY,
--     `session_id` VARCHAR(36) NOT NULL,
--     `latitude` DECIMAL(10, 7) NOT NULL,
--     `longitude` DECIMAL(10, 7) NOT NULL,
--     `stop_name` VARCHAR(128) NULL,
--     `arrived_at_utc` DATETIME NOT NULL,
--     INDEX `IX_tracking_stops_session_time` (`session_id`, `arrived_at_utc`),
--     FOREIGN KEY (`session_id`) REFERENCES `tracking_sessions` (`id`) ON DELETE CASCADE
-- ) ENGINE=InnoDB;
-- ==============================================================================
