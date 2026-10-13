package com.company.attendance.feature.calendar.detail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.company.attendance.design.components.DayDetailSheetContent
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.feature.calendar.model.DayDetailUiState

/**
 * DayDetailBottomSheet: Bottom sheet displaying attendance details for a selected calendar date.
 *
 * Displays:
 * - Selected date title (e.g. "Wednesday, 30 September 2026")
 * - Punch In time in office timezone (or "—" / "--:--")
 * - Punch Out time in office timezone (or "—" / "--:--")
 * - Status chip (COMPLETE, MISSED_PUNCH, NO_RECORD)
 * - "Time unverified" badge if time_unverified == true
 * - Sync status badge (SYNCED, PENDING, FAILED)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    detailState: DayDetailUiState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = AppSurface,
        dragHandle = null, // Drag handle is built into DayDetailSheetContent
        modifier = modifier
    ) {
        DayDetailSheetContent(
            dateTitle = detailState.dateFormatted,
            inTime = detailState.inTimeFormatted,
            outTime = detailState.outTimeFormatted,
            status = detailState.status,
            timeUnverified = detailState.timeUnverified,
            syncStatus = detailState.syncStatus,
            onCloseClick = onDismissRequest
        )
    }
}
