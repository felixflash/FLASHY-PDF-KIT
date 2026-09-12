package com.flashypdfkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.ui.components.ToolTopBar
import com.flashypdfkit.ui.theme.ActivePalette
import com.flashypdfkit.ui.theme.AppRadius
import com.flashypdfkit.ui.theme.AppSpacing

@Composable
fun LegalScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    val canvasColor = if (isDark) ActivePalette.DarkCanvas else ActivePalette.LightCanvas
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        ToolTopBar(title = "Privacy & Terms", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
                .verticalScroll(scrollState)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.lg),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = ActivePalette.Teal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacy Policy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "FlashyPDF Kit operates on a zero-telemetry, offline-first philosophy. All PDF rendering, merging, splitting, compressing, and signature stamping execute entirely on your Android device in a private, sandboxed environment. No document data, images, or passwords are ever transmitted to external servers or third parties.",
                        fontSize = 13.sp,
                        color = textMuted,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.lg),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = ActivePalette.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Terms of Service",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "By using FlashyPDF Kit, you agree to use the application for lawful PDF management purposes. The software is provided 'as is' without warranties of any kind. You maintain full ownership of all files and intellectual property processed through the application.",
                        fontSize = 13.sp,
                        color = textMuted,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

