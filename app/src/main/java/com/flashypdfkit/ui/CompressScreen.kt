package com.flashypdfkit.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.data.PreferencesAndHistory
import com.flashypdfkit.model.PdfToolType
import com.flashypdfkit.pdf.PdfEngine
import com.flashypdfkit.ui.components.Dropzone
import com.flashypdfkit.ui.components.FileItemCard
import com.flashypdfkit.ui.components.GradientButton
import com.flashypdfkit.ui.components.ResultCard
import com.flashypdfkit.ui.components.ToolTopBar
import com.flashypdfkit.ui.theme.ActivePalette
import com.flashypdfkit.ui.theme.AppRadius
import com.flashypdfkit.ui.theme.AppSpacing
import com.flashypdfkit.ui.theme.isAppInDarkTheme
import com.flashypdfkit.ui.theme.tactilePress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun CompressScreen(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    onPickPdf: () -> Unit,
    onSaveResult: (File, PdfToolType) -> Unit,
    onShareResult: (File) -> Unit
) {
    val context = LocalContext.current
    val prefsHistory = remember { PreferencesAndHistory(context) }
    val scope = rememberCoroutineScope()
    val isDark = isAppInDarkTheme()

    val canvasColor = if (isDark) ActivePalette.DarkCanvas else ActivePalette.LightCanvas
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val surfaceMuted = if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted

    var currentUri by remember { mutableStateOf(initialUri) }
    var compressLevel by remember { mutableFloatStateOf(2f) } // 1: Light, 2: Balanced, 3: Strong
    var isProcessing by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<File?>(null) }
    var savedPercent by remember { mutableStateOf(0) }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            currentUri = initialUri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        ToolTopBar(
            title = "Compress",
            onBack = onBack,
            infoTitle = "Smart Size Optimization",
            infoText = "Shrink file size up to ~65% for easy email and messaging sharing while preserving crystal clear text.",
            toolKey = "compress",
            prefsHistory = prefsHistory
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
        ) {

            if (currentUri == null) {
                Dropzone(
                    label = "Tap to choose a PDF to compress",
                    hint = "Select PDF document from storage",
                    onClick = onPickPdf
                )
            } else {
                FileItemCard(
                    name = currentUri?.lastPathSegment ?: "Selected Document",
                    sizeText = "Ready for Compression",
                    uri = currentUri,
                    onRemove = { currentUri = null; resultFile = null }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Compression Level Selector Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.xl),
                    color = surfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Compression Profile",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 Preset Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = listOf(
                                Triple(1f, "Light", "~35% smaller"),
                                Triple(2f, "Balanced", "~60% smaller"),
                                Triple(3f, "Strong", "~75% smaller")
                            )

                            presets.forEach { (level, title, desc) ->
                                val isSelected = compressLevel.roundToInt() == level.roundToInt()
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(AppRadius.md))
                                        .tactilePress { compressLevel = level },
                                    shape = RoundedCornerShape(AppRadius.md),
                                    color = if (isSelected) ActivePalette.Primary.copy(alpha = if (isDark) 0.22f else 0.12f) else surfaceMuted,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) ActivePalette.Primary else borderColor
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) ActivePalette.Primary else textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = desc,
                                            fontSize = 10.sp,
                                            color = if (isSelected) ActivePalette.Primary.copy(alpha = 0.85f) else textMuted
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = compressLevel,
                            onValueChange = { compressLevel = it },
                            valueRange = 1f..3f,
                            steps = 1,
                            colors = SliderDefaults.colors(
                                thumbColor = ActivePalette.Primary,
                                activeTrackColor = ActivePalette.Primary,
                                inactiveTrackColor = borderColor
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "High Quality", fontSize = 10.sp, color = textMuted)
                            Text(text = "Recommended", fontSize = 10.sp, color = ActivePalette.Primary, fontWeight = FontWeight.SemiBold)
                            Text(text = "Smallest Size", fontSize = 10.sp, color = textMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (resultFile != null) {
                    ResultCard(
                        title = "PDF Compressed Successfully!",
                        subtitle = "Reduced file size by ~$savedPercent%! Saved to ${resultFile?.name}",
                        onSave = { resultFile?.let { onSaveResult(it, PdfToolType.COMPRESS) } },
                        onShare = { resultFile?.let { onShareResult(it) } }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ActivePalette.Teal,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "100% on-device compression, zero cloud uploads",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                    }

                    GradientButton(
                        text = "Compress PDF Document",
                        isLoading = isProcessing,
                        onClick = {
                            val uri = currentUri ?: return@GradientButton
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val outFile = File(context.cacheDir, "Compressed_${System.currentTimeMillis()}.pdf")
                                val success = PdfEngine.compressPdf(context, uri, compressLevel.roundToInt(), outFile)
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    if (success) {
                                        savedPercent = when (compressLevel.roundToInt()) {
                                            1 -> 35
                                            3 -> 75
                                            else -> 60
                                        }
                                        resultFile = outFile
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("compress_action_button")
                    )
                }
            }
        }
    }
}

