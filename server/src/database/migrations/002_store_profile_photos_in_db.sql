-- ==============================================================================
-- Migration: Store Profile Photos in Database
-- Target: Microsoft SQL Server
-- Description: Adds [image_data] and [mime_type] columns to [user_photos] table.
-- Safe to run multiple times (idempotent). Does not alter or remove existing rows.
-- ==============================================================================

-- 1. Add [image_data] column to [user_photos] table if not exists
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('[dbo].[user_photos]') AND name = 'image_data'
)
BEGIN
    ALTER TABLE [dbo].[user_photos]
    ADD [image_data] VARBINARY(MAX) NULL;
END;

-- 2. Add [mime_type] column to [user_photos] table if not exists
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('[dbo].[user_photos]') AND name = 'mime_type'
)
BEGIN
    ALTER TABLE [dbo].[user_photos]
    ADD [mime_type] VARCHAR(32) NULL;
END;

-- ==============================================================================
-- Verification Query:
-- SELECT column_name, data_type, character_maximum_length 
-- FROM information_schema.columns 
-- WHERE table_name = 'user_photos';
--
-- MySQL equivalent (for reference):
-- ALTER TABLE `user_photos` ADD COLUMN `image_data` LONGBLOB NULL;
-- ALTER TABLE `user_photos` ADD COLUMN `mime_type` VARCHAR(32) NULL;
-- ==============================================================================
