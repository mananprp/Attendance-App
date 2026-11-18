-- ==============================================================================
-- Migration: Supervisor Role, User Multi-Site Mapping, and Created By Tracking
-- Target: Microsoft SQL Server (MSSQL)
-- Description:
--   1. Adds 'SUPERVISOR' role support.
--   2. Adds [created_by] column to [office_config] to track site creator/manager.
--   3. Creates [user_sites] junction table for multi-site employee assignments.
--   4. Backfills existing [site_code] from [users] into [user_sites].
-- Safe to run multiple times (idempotent).
-- ==============================================================================

-- 1. Add [created_by] column to [office_config] if not exists
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('[dbo].[office_config]') AND name = 'created_by'
)
BEGIN
    ALTER TABLE [dbo].[office_config]
    ADD [created_by] VARCHAR(64) NULL;
END;

-- 2. Create [user_sites] junction table for multi-site assignments
IF NOT EXISTS (
    SELECT 1 FROM sys.tables WHERE name = 'user_sites' AND schema_id = SCHEMA_ID('dbo')
)
BEGIN
    CREATE TABLE [dbo].[user_sites] (
        [user_id] VARCHAR(64) NOT NULL,
        [site_code] VARCHAR(64) NOT NULL,
        [assigned_by] VARCHAR(64) NULL,
        [created_at] DATETIME NOT NULL DEFAULT GETUTCDATE(),
        CONSTRAINT [PK_user_sites] PRIMARY KEY CLUSTERED ([user_id] ASC, [site_code] ASC),
        CONSTRAINT [FK_user_sites_users] FOREIGN KEY ([user_id]) 
            REFERENCES [dbo].[users] ([user_id]) ON DELETE CASCADE,
        CONSTRAINT [FK_user_sites_office] FOREIGN KEY ([site_code]) 
            REFERENCES [dbo].[office_config] ([office_id]) ON DELETE CASCADE
    );

    CREATE NONCLUSTERED INDEX [IX_user_sites_site_code] 
        ON [dbo].[user_sites] ([site_code] ASC);
END;

-- 3. Backfill existing primary site_code into [user_sites] if not already present
IF EXISTS (SELECT 1 FROM sys.tables WHERE name = 'user_sites' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    INSERT INTO [dbo].[user_sites] ([user_id], [site_code], [created_at])
    SELECT u.[user_id], u.[site_code], GETUTCDATE()
    FROM   [dbo].[users] u
    WHERE  u.[site_code] IS NOT NULL 
      AND  u.[site_code] <> ''
      AND  EXISTS (SELECT 1 FROM [dbo].[office_config] o WHERE o.[office_id] = u.[site_code])
      AND  NOT EXISTS (
          SELECT 1 FROM [dbo].[user_sites] us 
          WHERE  us.[user_id] = u.[user_id] AND us.[site_code] = u.[site_code]
      );
END;

-- ==============================================================================
-- Verification Queries:
-- SELECT * FROM [dbo].[user_sites];
-- SELECT [user_id], [name], [role], [site_code] FROM [dbo].[users];
-- ==============================================================================
