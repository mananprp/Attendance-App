package com.company.attendance.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabled
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTokensTest {

    @Test
    fun `verify exact PRD Section 8 color palette hex values`() {
        assertEquals(Color(0xFFFAFAFA), AppBackground)
        assertEquals(Color(0xFFFFFFFF), AppSurface)
        assertEquals(Color(0xFF111827), AppPrimaryText)
        assertEquals(Color(0xFF6B7280), AppSecondaryText)
        assertEquals(Color(0xFFE5E7EB), AppBorder)
        assertEquals(Color(0xFF2563EB), AppAccent)
        assertEquals(Color(0xFFEFF4FF), AppAccentTint)
        assertEquals(Color(0xFF16A34A), AppSuccess)
        assertEquals(Color(0xFFD97706), AppWarning)
        assertEquals(Color(0xFFDC2626), AppError)
    }

    @Test
    fun `verify typography font sizes`() {
        assertEquals(40.sp, AppTypographyTokens.ClockTime.fontSize)
        assertEquals(22.sp, AppTypographyTokens.Title.fontSize)
        assertEquals(16.sp, AppTypographyTokens.SectionHeading.fontSize)
        assertEquals(14.sp, AppTypographyTokens.Body.fontSize)
        assertEquals(12.sp, AppTypographyTokens.Caption.fontSize)
    }

    @Test
    fun `verify design system dimensions`() {
        assertEquals(20.dp, AppDimensions.ScreenPadding)
        assertEquals(52.dp, AppDimensions.ButtonHeight)
        assertEquals(12.dp, AppDimensions.ButtonCornerRadius)
        assertEquals(12.dp, AppDimensions.CardCornerRadius)
        assertEquals(1.dp, AppDimensions.BorderWidth)
        assertEquals(48.dp, AppDimensions.MinTouchTarget)
    }

    @Test
    fun `verify DayStatus chip mapping availability`() {
        val statuses = DayStatus.values()
        assertEquals(3, statuses.size)
        assertTrue(statuses.contains(DayStatus.COMPLETE))
        assertTrue(statuses.contains(DayStatus.MISSED_PUNCH))
        assertTrue(statuses.contains(DayStatus.NO_RECORD))
    }

    @Test
    fun `verify SyncStatus mapping availability`() {
        val syncStatuses = SyncStatus.values()
        assertEquals(3, syncStatuses.size)
        assertTrue(syncStatuses.contains(SyncStatus.PENDING))
        assertTrue(syncStatuses.contains(SyncStatus.SYNCED))
        assertTrue(syncStatuses.contains(SyncStatus.FAILED))
    }
}
