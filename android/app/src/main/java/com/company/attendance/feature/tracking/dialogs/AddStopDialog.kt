package com.company.attendance.feature.tracking.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.components.SecondaryButton
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStopDialog(
    onDismiss: () -> Unit,
    onConfirm: (stopName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var stopName by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AppSurface,
            border = BorderStroke(AppDimensions.BorderWidth, AppBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.Spacing24)
            ) {
                Text(
                    text = "Record Site Stop",
                    style = AppTypographyTokens.Title.copy(fontWeight = FontWeight.Bold),
                    color = AppPrimaryText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Log a checkpoint or inspection milestone at your current coordinates.",
                    style = AppTypographyTokens.Caption,
                    color = AppSecondaryText
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                OutlinedTextField(
                    value = stopName,
                    onValueChange = { stopName = it },
                    placeholder = { Text("e.g. Site Visit #2, Cement Check", color = AppSecondaryText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = AppBorder,
                        focusedTextColor = AppPrimaryText,
                        unfocusedTextColor = AppPrimaryText,
                        cursorColor = AppAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(AppDimensions.Spacing12))

                    PrimaryButton(
                        text = "Add Stop",
                        onClick = {
                            onConfirm(stopName.ifBlank { "Site Stop" })
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
