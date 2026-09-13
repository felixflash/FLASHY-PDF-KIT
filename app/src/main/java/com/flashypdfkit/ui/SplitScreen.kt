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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.data.PreferencesAndHistory
import com.flashypdfkit.model.PdfToolType
import com.flashypdfkit.pdf.PdfEngine
import com.flashypdfkit.ui.components.Dropzone
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

@Composable
fun SplitScreen(
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
    var totalPages by remember { mutableIntStateOf(0) }
    var rangeText by remember { mutableStateOf("1") }
    var isProcessing by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            currentUri = initialUri
        }
    }

    LaunchedEffect(currentUri) {
        val uri = currentUri ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            totalPages = PdfEngine.getPdfPageCount(context, uri)
            if (totalPages > 1) {
                rangeText = "1-${totalPages / 2}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        ToolTopBar(
            title = "Split",
            onBack = onBack,
            infoTitle = "Extract Specific Pages",
            infoText = "Extract only the pages you need. Select custom page ranges (e.g., 1, 3, 5-8) into a crisp new PDF.",
            toolKey = "split",
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
                    label = "Tap to choose a PDF to split",
                    hint = "Select document to extract pages from",
                    onClick = onPickPdf
                )
            } else {
                // Selected file summary card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.md),
                    color = surfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.flashypdfkit.ui.components.FilePreviewThumbnail(
                            context = androidx.compose.ui.platform.LocalContext.current,
                            uri = currentUri!!,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(AppRadius.sm))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUri?.lastPathSegment?.substringAfterLast("/") ?: "Document",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (totalPages > 0) "$totalPages Total Pages Detected" else "Analyzing pages…",
                                fontSize = 12.sp,
                                color = textMuted
                            )
                        }

                        IconButton(
                            onClick = {
                                currentUri = null
                                resultFile = null
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(surfaceMuted)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove file",
                                tint = textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Page grid preview (Organize style)
                Text(
                    text = "Visual Page Preview:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(totalPages) { idx ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(AppRadius.sm))
                                .border(1.dp, borderColor, RoundedCornerShape(AppRadius.sm))
                                .tactilePress {
                                    // Append or toggle page in rangeText
                                    val currentRange = rangeText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                                    if (currentRange.contains("${idx + 1}")) {
                                        currentRange.remove("${idx + 1}")
                                    } else {
                                        currentRange.add("${idx + 1}")
                                    }
                                    rangeText = currentRange.joinToString(", ")
                                }
                        ) {
                            com.flashypdfkit.ui.components.FilePreviewThumbnail(
                                context = context,
                                uri = currentUri!!,
                                pageIndex = idx,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Selection overlay
                            val isSelected = rangeText.split(",").map { it.trim() }.contains("${idx + 1}")
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(ActivePalette.Primary.copy(alpha = 0.25f))
                                        .border(2.dp, ActivePalette.Primary, RoundedCornerShape(AppRadius.sm))
                                )
                            }

                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                shape = CircleShape,
                                color = if (isSelected) ActivePalette.Primary else Color.Black.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Pages to extract:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))


                OutlinedTextField(
                    value = rangeText,
                    onValueChange = { rangeText = it },
                    placeholder = {
                        Text(
                            "e.g. 1, 3, 5-8",
                            fontSize = 14.sp,
                            color = textMuted.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.md),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ActivePalette.Teal,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick presets row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SplitPresetButton(
                        text = "Odd Pages",
                        onClick = {
                            val list = (1..totalPages).filter { it % 2 != 0 }
                            rangeText = list.joinToString(",")
                        },
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )

                    SplitPresetButton(
                        text = "Even Pages",
                        onClick = {
                            val list = (1..totalPages).filter { it % 2 == 0 }
                            rangeText = list.joinToString(",")
                        },
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )

                    SplitPresetButton(
                        text = "All Pages",
                        onClick = {
                            rangeText = "1-$totalPages"
                        },
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (resultFile != null) {
                    ResultCard(
                        title = "PDF Split Complete!",
                        subtitle = "Extracted chosen pages into ${resultFile?.name}",
                        onSave = { resultFile?.let { onSaveResult(it, PdfToolType.SPLIT) } },
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
                            text = "Extracts only desired pages on-device",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                    }

                    GradientButton(
                        text = "Split & Save PDF",
                        enabled = currentUri != null && rangeText.isNotBlank(),
                        isLoading = isProcessing,
                        onClick = {
                            val uri = currentUri ?: return@GradientButton
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val pageNums = parsePageRanges(rangeText, totalPages)
                                val outFile = File(context.cacheDir, "Split_${System.currentTimeMillis()}.pdf")
                                val success = PdfEngine.splitPdf(context, uri, pageNums, outFile)
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    if (success) {
                                        resultFile = outFile
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("split_action_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitPresetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .tactilePress(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.sm),
        color = if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) ActivePalette.DarkBorder else Color(0xFFDFE3EB)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
            )
        }
    }
}

private fun parsePageRanges(input: String, maxPage: Int): List<Int> {
    val result = mutableSetOf<Int>()
    val parts = input.split(",")
    for (part in parts) {
        val trimmed = part.trim()
        if (trimmed.contains("-")) {
            val rangeParts = trimmed.split("-")
            if (rangeParts.size == 2) {
                val start = rangeParts[0].toIntOrNull() ?: 1
                val end = rangeParts[1].toIntOrNull() ?: maxPage
                for (i in start..end) {
                    if (i in 1..maxPage) result.add(i)
                }
            }
        } else {
            val num = trimmed.toIntOrNull()
            if (num != null && num in 1..maxPage) {
                result.add(num)
            }
        }
    }
    return result.sorted()
}

