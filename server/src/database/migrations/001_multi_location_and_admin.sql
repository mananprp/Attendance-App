-- ==============================================================================
-- Migration: Multi-Location Support, Match Threshold Accuracy, and Admin Role
-- Target: Microsoft SQL Server
-- ==============================================================================

-- 1. Accuracy Fix: Update match_threshold to 0.46 for ALL office sites
UPDATE [dbo].[office_config]
SET    [match_threshold] = 0.46;

-- 2. Insert Remote Site 1 (Placeholder - fill in actual coords, name, and radius)
IF NOT EXISTS (SELECT 1 FROM [dbo].[office_config] WHERE [office_id] = 'SITE_REMOTE_001')
BEGIN
    INSERT INTO [dbo].[office_config]
      ([office_id], [name], [latitude], [longitude], [radius_m], [timezone],
       [sync_windows], [sync_jitter_minutes], [match_threshold])
    VALUES
      (
        'SITE_REMOTE_001',          -- Office / Site ID
        'Remote Site 1',            -- Site display name
        28.4595000,                 -- <-- UPDATE: Latitude
        77.0266000,                 -- <-- UPDATE: Longitude
        150,                        -- Radius in meters (recommended >= 150 for remote sites)
        'Asia/Kolkata',             -- Timezone
        '["16:00","00:00"]',        -- Sync windows
        30,                         -- Sync jitter minutes
        0.46                        -- Match threshold
      );
END;

-- 3. Add [role] column to [users] table if not exists
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('[dbo].[users]') AND name = 'role'
)
BEGIN
    ALTER TABLE [dbo].[users]
    ADD [role] VARCHAR(16) NOT NULL
        CONSTRAINT [DF_users_role] DEFAULT 'EMPLOYEE';
END;

-- 4. Add [site_code] column to [users] table if not exists
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('[dbo].[users]') AND name = 'site_code'
)
BEGIN
    ALTER TABLE [dbo].[users]
    ADD [site_code] VARCHAR(64) NOT NULL
        CONSTRAINT [DF_users_site_code] DEFAULT 'DEFAULT_OFFICE';
END;

-- 5. Promote an Admin User (Replace 'YOUR_ADMIN_USER_ID' with your actual user_id, e.g. 'ADMIN001' or your login ID)
-- UPDATE [dbo].[users]
-- SET    [role] = 'ADMIN'
-- WHERE  [user_id] = 'YOUR_ADMIN_USER_ID';

-- 6. Example: Assign specific employees to remote site
-- UPDATE [dbo].[users]
-- SET    [site_code] = 'SITE_REMOTE_001'
-- WHERE  [user_id] IN ('EMP001', 'EMP002');

-- Verification Queries:
SELECT [office_id], [name], [latitude], [longitude], [radius_m], [match_threshold] 
FROM [dbo].[office_config];

SELECT [user_id], [name], [role], [site_code], [status] 
FROM [dbo].[users];
