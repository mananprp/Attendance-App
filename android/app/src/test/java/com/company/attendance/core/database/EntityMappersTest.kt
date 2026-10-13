package com.company.attendance.core.database

import com.company.attendance.core.database.EntityMappers.toDomain
import com.company.attendance.core.database.EntityMappers.toEntity
import com.company.attendance.core.database.EntityMappers.toUserSession
import com.company.attendance.core.database.entity.AppConfigEntity
import com.company.attendance.core.database.entity.DaySummaryEntity
import com.company.attendance.core.database.entity.PunchEntity
import com.company.attendance.core.database.entity.UserLocalEntity
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.Punch
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.core.model.TimeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMappersTest {

    @Test
    fun `UserLocalEntity maps to UserSession correctly`() {
        val entity = UserLocalEntity(
            userId = "EMP1001",
            displayName = "Alice Sharma",
            photoChangeUsed = true,
            hasPhoto = true,
            referenceEmbedding = byteArrayOf(1, 2, 3),
            updatedAt = 1727670000000L
        )

        val session = entity.toUserSession("token-abc")
        assertEquals("EMP1001", session.userId)
        assertEquals("Alice Sharma", session.displayName)
        assertEquals("token-abc", session.token)
        assertTrue(session.photoChangeUsed)
        assertTrue(session.hasPhoto)
    }

    @Test
    fun `AppConfigEntity bidirectional mapping preserves fields and JSON list`() {
        val domain = AppConfig(
            officeLat = 28.613939,
            officeLng = 77.209021,
            radiusMeters = 100,
            timezone = "Asia/Kolkata",
            syncWindows = listOf("16:00", "00:00"),
            syncJitterMinutes = 30,
            matchThreshold = 0.55f,
            lastFetchedAt = 1727670000000L
        )

        val entity = domain.toEntity()
        assertEquals(28.613939, entity.officeLat, 0.00001)
        assertEquals(100, entity.radiusMeters)

        val mappedDomain = entity.toDomain()
        assertEquals(domain.officeLat, mappedDomain.officeLat, 0.00001)
        assertEquals(domain.syncWindows, mappedDomain.syncWindows)
        assertEquals(domain.matchThreshold, mappedDomain.matchThreshold, 0.001f)
    }

    @Test
    fun `PunchEntity bidirectional mapping preserves values`() {
        val punch = Punch(
            punchUuid = "uuid-1234",
            userId = "EMP1001",
            dateLocal = "2026-09-30",
            type = PunchType.IN,
            punchTimeUtcEpochMs = 1727670000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 12.5f,
            matchScore = 0.89f,
            imagePath = "/path/to/punch.jpg",
            syncStatus = SyncStatus.PENDING,
            syncAttempts = 0,
            createdAtEpochMs = 1727670000000L
        )

        val entity = punch.toEntity()
        assertEquals("uuid-1234", entity.punchUuid)
        assertEquals("IN", entity.type)
        assertEquals("NTP", entity.timeSource)

        val mapped = entity.toDomain()
        assertEquals(punch.punchUuid, mapped.punchUuid)
        assertEquals(PunchType.IN, mapped.type)
        assertEquals(TimeSource.NTP, mapped.timeSource)
        assertEquals(12.5f, mapped.distanceMeters, 0.01f)
    }

    @Test
    fun `DaySummaryEntity bidirectional mapping preserves values`() {
        val summary = DaySummary(
            userId = "EMP1001",
            dateLocal = "2026-09-30",
            inTimeUtcEpochMs = 1727670000000L,
            outTimeUtcEpochMs = 1727700000000L,
            status = DayStatus.COMPLETE,
            timeUnverified = false
        )

        val entity = summary.toEntity()
        assertEquals("COMPLETE", entity.status)

        val mapped = entity.toDomain()
        assertEquals(summary.userId, mapped.userId)
        assertEquals(summary.dateLocal, mapped.dateLocal)
        assertEquals(DayStatus.COMPLETE, mapped.status)
        assertFalse(mapped.timeUnverified)
    }
}
