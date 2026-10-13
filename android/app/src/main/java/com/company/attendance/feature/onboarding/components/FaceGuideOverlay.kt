package com.company.attendance.feature.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppTypographyTokens

@Composable
fun FaceGuideOverlay(
    modifier: Modifier = Modifier,
    instruction: String = "Align your face inside the oval guide",
    borderColor: Color = AppAccent
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.99f) // Required for PorterDuff BlendMode.Clear
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val ovalWidth = canvasWidth * 0.70f
            val ovalHeight = ovalWidth * 1.35f
            val left = (canvasWidth - ovalWidth) / 2f
            val top = (canvasHeight - ovalHeight) / 2.4f

            // Dark semi-transparent scrim
            drawRect(
                color = Color.Black.copy(alpha = 0.55f),
                size = size
            )

            // Transparent oval cutout
            drawOval(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(ovalWidth, ovalHeight),
                blendMode = BlendMode.Clear
            )

            // Oval stroke border
            drawOval(
                color = borderColor,
                topLeft = Offset(left, top),
                size = Size(ovalWidth, ovalHeight),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Text(
            text = instruction,
            style = AppTypographyTokens.BodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AppDimensions.Spacing32)
                .padding(horizontal = AppDimensions.ScreenPadding)
        )
    }
}
