package com.company.attendance.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Shape definitions for Attendance App (Findez dark theme: 20dp cards, 12dp chips, 32dp body panel).
 */
val AttendanceShapes = Shapes(
    small = RoundedCornerShape(AppDimensions.ChipCornerRadius), // 12dp
    medium = RoundedCornerShape(AppDimensions.CardCornerRadius), // 20dp
    large = RoundedCornerShape(AppDimensions.BodyPanelTopCornerRadius) // 32dp
)
