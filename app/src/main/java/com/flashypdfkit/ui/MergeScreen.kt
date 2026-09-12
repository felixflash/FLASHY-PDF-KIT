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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
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
import com.flashypdfkit.ui.theme.tactilePress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MergeScreen(
    selectedUris: List<Uri>,
    onBack: () -> Unit,
    onPickFiles: () -> Unit,
    onSaveResult: (File, PdfToolType) -> Unit,
    onShareResult: (File) -> Unit
) {
    val context = LocalContext.current
    val prefsHistory = remember { PreferencesAndHistory(context) }
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val canvasColor = if (isDark) ActivePalette.DarkCanvas else ActivePalette.LightCanvas
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val surfaceMuted = if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted

    val fileList = remember { mutableStateListOf<Uri>().apply { addAll(selectedUris) } }
    var isProcessing by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(selectedUris.toList()) {
        if (selectedUris.isNotEmpty()) {
            fileList.clear()
            fileList.addAll(selectedUris)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        ToolTopBar(
            title = "Merge",
            onBack = onBack,
            infoTitle = "Combine in Sequence",
            infoText = "Files are stitched in the order listed below into a single PDF.",
            toolKey = "merge",
            prefsHistory = prefsHistory
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
        ) {
            val userSettings = prefsHistory.getUserSettings()
            if (!userSettings.isPremium) {
                val uses = com.flashypdfkit.ads.UsageManager.getMergeUses(context)
                val extra = com.flashypdfkit.ads.UsageManager.getMergeRewardedExtra(context)
                val remaining = (com.flashypdfkit.ads.UsageManager.MAX_MERGE_FREE - uses).coerceAtLeast(0) + extra
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppSpacing.md),
                    shape = RoundedCornerShape(AppRadius.md),
                    colors = CardDefaults.cardColors(containerColor = surfaceMuted),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ Free Tier Daily Limit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMuted
                        )
                        Text(
                            text = "$remaining uses remaining today",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ActivePalette.Primary
                        )
                    }
                }
            }

            // Main dropzone / add files
            if (fileList.isEmpty()) {
                Dropzone(
                    label = "Tap to choose PDF documents",
                    hint = "Select 2 or more files from your device to merge",
                    onClick = onPickFiles
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${fileList.size} Files Added",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    // Add more button
                    OutlinedButton(
                        onClick = onPickFiles,
                        shape = RoundedCornerShape(AppRadius.full),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ActivePalette.Primary.copy(alpha = 0.5f)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.tactilePress(onClick = onPickFiles)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ActivePalette.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add More",
                            color = ActivePalette.Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sequence List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(fileList) { index, uri ->
                    val name = uri.lastPathSegment?.substringAfterLast("/") ?: "Document ${index + 1}"
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.md),
                        color = surfaceColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail preview
                            com.flashypdfkit.ui.components.FilePreviewThumbnail(
                                context = androidx.compose.ui.platform.LocalContext.current,
                                uri = uri,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(AppRadius.sm))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Sequence Part #${index + 1}",
                                    fontSize = 11.sp,
                                    color = textMuted
                                )
                            }

                            IconButton(
                                onClick = { fileList.removeAt(index) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(AppRadius.sm))
                                    .background(ActivePalette.Danger.copy(alpha = if (isDark) 0.18f else 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove file",
                                    tint = ActivePalette.Danger,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom action / result area
            Spacer(modifier = Modifier.height(10.dp))

            if (resultFile != null) {
                ResultCard(
                    title = "PDFs Merged Successfully!",
                    subtitle = "Combined ${fileList.size} files into ${resultFile?.name}",
                    onSave = { resultFile?.let { onSaveResult(it, PdfToolType.MERGE) } },
                    onShare = { resultFile?.let { onShareResult(it) } }
                )
            } else {
                // Reassurance microcopy
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
                        text = "100% on-device • No files uploaded to any server",
                        fontSize = 11.sp,
                        color = textMuted
                    )
                }

                GradientButton(
                    text = if (fileList.size >= 2) "Merge ${fileList.size} PDFs" else "Select At Least 2 PDFs",
                    enabled = fileList.size >= 2,
                    isLoading = isProcessing,
                    onClick = {
                        isProcessing = true
                        scope.launch(Dispatchers.IO) {
                            val outFile = File(context.cacheDir, "Merged_${System.currentTimeMillis()}.pdf")
                            val success = PdfEngine.mergePdfs(context, fileList, outFile)
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                if (success) {
                                    resultFile = outFile
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("merge_action_button")
                )
            }
        }
    }
}

