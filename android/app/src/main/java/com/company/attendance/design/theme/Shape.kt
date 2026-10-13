package com.company.attendance.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape definitions for Attendance App (12dp corners for cards and buttons, 8dp for chips).
 */
val AttendanceShapes = Shapes(
    small = RoundedCornerShape(AppDimensions.ChipCornerRadius), // 8dp
    medium = RoundedCornerShape(AppDimensions.CardCornerRadius), // 12dp
    large = RoundedCornerShape(AppDimensions.CardCornerRadius)   // 12dp
)
