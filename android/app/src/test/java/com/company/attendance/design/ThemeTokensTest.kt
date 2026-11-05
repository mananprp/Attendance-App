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
import com.company.attendance.design.theme.AppHeaderBackground
import com.company.attendance.design.theme.AppOnAccent
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
    fun `verify exact Findez dark theme color palette hex values`() {
        assertEquals(Color(0xFF070B0F), AppBackground)
        assertEquals(Color(0xFF14181D), AppHeaderBackground)
        assertEquals(Color(0xFF14181D), AppSurface)
        assertEquals(Color(0xFFFFFFFF), AppPrimaryText)
        assertEquals(Color(0xFFA3A9B0), AppSecondaryText)
        assertEquals(Color(0xFF2A2E33), AppBorder)
        assertEquals(Color(0xFF4EFFCA), AppAccent)
        assertEquals(Color(0x244EFFCA), AppAccentTint)
        assertEquals(Color(0xFF04120D), AppOnAccent)
        assertEquals(Color(0xFF4EFFCA), AppSuccess)
        assertEquals(Color(0xFFFBBF24), AppWarning)
        assertEquals(Color(0xFFF87171), AppError)
    }

    @Test
    fun `verify typography font sizes`() {
        assertEquals(48.sp, AppTypographyTokens.ClockTime.fontSize)
        assertEquals(22.sp, AppTypographyTokens.Title.fontSize)
        assertEquals(22.sp, AppTypographyTokens.StatValue.fontSize)
        assertEquals(16.sp, AppTypographyTokens.SectionHeading.fontSize)
        assertEquals(14.sp, AppTypographyTokens.Body.fontSize)
        assertEquals(14.sp, AppTypographyTokens.RingCaption.fontSize)
        assertEquals(13.sp, AppTypographyTokens.StatLabel.fontSize)
        assertEquals(12.sp, AppTypographyTokens.Caption.fontSize)
        assertEquals(12.sp, AppTypographyTokens.NavLabel.fontSize)
    }

    @Test
    fun `verify design system dimensions`() {
        assertEquals(20.dp, AppDimensions.ScreenPadding)
        assertEquals(52.dp, AppDimensions.ButtonHeight)
        assertEquals(26.dp, AppDimensions.ButtonCornerRadius)
        assertEquals(20.dp, AppDimensions.CardCornerRadius)
        assertEquals(12.dp, AppDimensions.ChipCornerRadius)
        assertEquals(32.dp, AppDimensions.BodyPanelTopCornerRadius)
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
