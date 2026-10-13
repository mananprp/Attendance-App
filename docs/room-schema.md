# Room / SQLite Database Schema Definition

This document outlines the SQLite schema used in the Android Room Database (`attendance_app.db`).

---

## 1. Database Configuration

- **Database Name:** `attendance_app.db`
- **Room Database Version:** `1`
- **Schema Export:** Enabled (`room.schemaLocation` in build config)
- **Entities:**
  1. `UserLocalEntity` (`user_local`)
  2. `PunchEntity` (`punches`)
  3. `DaySummaryEntity` (`day_summary`)
  4. `AppConfigEntity` (`app_config`)

---

## 2. Entities & Table Schemas

### 2.1 Table: `user_local`
Caches the authenticated employee's local state, reference embedding (from profile photo v1/v2), and whether their one-time photo change has been consumed.

```sql
CREATE TABLE IF NOT EXISTS `user_local` (
    `user_id` TEXT NOT NULL,
    `display_name` TEXT NOT NULL,
    `photo_change_used` INTEGER NOT NULL DEFAULT 0,
    `has_photo` INTEGER NOT NULL DEFAULT 0,
    `reference_embedding` BLOB,
    `updated_at` INTEGER NOT NULL,
    PRIMARY KEY(`user_id`)
);
```

**Kotlin Entity Mapping:**
```kotlin
@Entity(tableName = "user_local")
data class UserLocalEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "photo_change_used") val photoChangeUsed: Boolean,
    @ColumnInfo(name = "has_photo") val hasPhoto: Boolean,
    @ColumnInfo(name = "reference_embedding") val referenceEmbedding: ByteArray?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
```

---

### 2.2 Table: `punches`
Stores every local punch attempt. All punches start with `sync_status = 'PENDING'`. Once the backend acknowledges the punch via `/attendance/sync`, it is updated to `SYNCED`.

```sql
CREATE TABLE IF NOT EXISTS `punches` (
    `punch_uuid` TEXT NOT NULL,
    `user_id` TEXT NOT NULL,
    `date_local` TEXT NOT NULL,
    `type` TEXT NOT NULL,
    `punch_time_utc` INTEGER NOT NULL,
    `time_source` TEXT NOT NULL,
    `time_unverified` INTEGER NOT NULL DEFAULT 0,
    `latitude` REAL NOT NULL,
    `longitude` REAL NOT NULL,
    `distance_m` REAL NOT NULL,
    `match_score` REAL NOT NULL,
    `image_path` TEXT,
    `sync_status` TEXT NOT NULL DEFAULT 'PENDING',
    `sync_attempts` INTEGER NOT NULL DEFAULT 0,
    `created_at` INTEGER NOT NULL,
    PRIMARY KEY(`punch_uuid`)
);

CREATE INDEX IF NOT EXISTS `index_punches_user_date` ON `punches` (`user_id`, `date_local`);
CREATE INDEX IF NOT EXISTS `index_punches_sync_status` ON `punches` (`sync_status`);
```

**Kotlin Entity Mapping:**
```kotlin
@Entity(
    tableName = "punches",
    indices = [
        Index(value = ["user_id", "date_local"]),
        Index(value = ["sync_status"])
    ]
)
data class PunchEntity(
    @PrimaryKey
    @ColumnInfo(name = "punch_uuid") val punchUuid: String, // UUID v4
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "date_local") val dateLocal: String, // YYYY-MM-DD
    @ColumnInfo(name = "type") val type: String, // IN, OUT
    @ColumnInfo(name = "punch_time_utc") val punchTimeUtc: Long, // Epoch ms (UTC)
    @ColumnInfo(name = "time_source") val timeSource: String, // NTP, DEVICE
    @ColumnInfo(name = "time_unverified") val timeUnverified: Boolean,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "distance_m") val distanceMeters: Float,
    @ColumnInfo(name = "match_score") val matchScore: Float,
    @ColumnInfo(name = "image_path") val imagePath: String?,
    @ColumnInfo(name = "sync_status") val syncStatus: String, // PENDING, SYNCED, FAILED
    @ColumnInfo(name = "sync_attempts") val syncAttempts: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

---

### 2.3 Table: `day_summary`
Aggregates the daily status for the local user to power the home screen state machine and the monthly Calendar screen.

```sql
CREATE TABLE IF NOT EXISTS `day_summary` (
    `user_id` TEXT NOT NULL,
    `date_local` TEXT NOT NULL,
    `in_time_utc` INTEGER,
    `out_time_utc` INTEGER,
    `status` TEXT NOT NULL DEFAULT 'NO_RECORD',
    `time_unverified` INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY(`user_id`, `date_local`)
);

CREATE INDEX IF NOT EXISTS `index_day_summary_user_date` ON `day_summary` (`user_id`, `date_local`);
```

**Kotlin Entity Mapping:**
```kotlin
@Entity(
    tableName = "day_summary",
    primaryKeys = ["user_id", "date_local"],
    indices = [
        Index(value = ["user_id", "date_local"])
    ]
)
data class DaySummaryEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "date_local") val dateLocal: String, // YYYY-MM-DD
    @ColumnInfo(name = "in_time_utc") val inTimeUtc: Long?,
    @ColumnInfo(name = "out_time_utc") val outTimeUtc: Long?,
    @ColumnInfo(name = "status") val status: String, // COMPLETE, MISSED_PUNCH, NO_RECORD
    @ColumnInfo(name = "time_unverified") val timeUnverified: Boolean = false
)
```

---

### 2.4 Table: `app_config`
Caches server configuration fetched from `/config`. Stores office coordinates, geofence radius, timezone, sync schedule windows, jitter, and face recognition threshold.

```sql
CREATE TABLE IF NOT EXISTS `app_config` (
    `id` INTEGER NOT NULL DEFAULT 1,
    `office_lat` REAL NOT NULL,
    `office_lng` REAL NOT NULL,
    `radius_m` INTEGER NOT NULL DEFAULT 100,
    `timezone` TEXT NOT NULL DEFAULT 'Asia/Kolkata',
    `sync_windows_json` TEXT NOT NULL,
    `sync_jitter_minutes` INTEGER NOT NULL DEFAULT 30,
    `match_threshold` REAL NOT NULL DEFAULT 0.55,
    `last_fetched_at` INTEGER NOT NULL,
    PRIMARY KEY(`id`)
);
```

**Kotlin Entity Mapping:**
```kotlin
@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int = 1,
    @ColumnInfo(name = "office_lat") val officeLat: Double,
    @ColumnInfo(name = "office_lng") val officeLng: Double,
    @ColumnInfo(name = "radius_m") val radiusMeters: Int = 100,
    @ColumnInfo(name = "timezone") val timezone: String = "Asia/Kolkata",
    @ColumnInfo(name = "sync_windows_json") val syncWindowsJson: String = "[\"16:00\", \"00:00\"]",
    @ColumnInfo(name = "sync_jitter_minutes") val syncJitterMinutes: Int = 30,
    @ColumnInfo(name = "match_threshold") val matchThreshold: Float = 0.55f,
    @ColumnInfo(name = "last_fetched_at") val lastFetchedAt: Long
)
```

---

## 3. Data Access Objects (DAOs)

1. **UserLocalDao:**
   - `getUser(userId: String): Flow<UserLocalEntity?>`
   - `insertOrUpdate(user: UserLocalEntity)`
   - `updatePhotoChangeUsed(userId: String, used: Boolean)`
   - `updateReferenceEmbedding(userId: String, embedding: ByteArray)`
   - `clearAll()`

2. **PunchDao:**
   - `getPunch(punchUuid: String): PunchEntity?`
   - `getPunchesForDate(userId: String, dateLocal: String): Flow<List<PunchEntity>>`
   - `getPendingPunches(userId: String): List<PunchEntity>`
   - `insertPunch(punch: PunchEntity)`
   - `updateSyncStatus(punchUuid: String, status: String, attempts: Int)`
   - `markPunchesSynced(punchUuids: List<String>)`

3. **DaySummaryDao:**
   - `getDaySummary(userId: String, dateLocal: String): Flow<DaySummaryEntity?>`
   - `getDaySummaryDirect(userId: String, dateLocal: String): DaySummaryEntity?`
   - `getMonthSummaries(userId: String, monthPrefix: String): Flow<List<DaySummaryEntity>>`
   - `insertOrUpdate(daySummary: DaySummaryEntity)`
   - `insertAll(summaries: List<DaySummaryEntity>)`

4. **AppConfigDao:**
   - `getConfig(): Flow<AppConfigEntity?>`
   - `getConfigDirect(): AppConfigEntity?`
   - `insertOrUpdate(config: AppConfigEntity)`
