package com.flashypdfkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.model.PdfToolType
import com.flashypdfkit.model.ProcessedFile
import com.flashypdfkit.ui.components.ToolTopBar
import com.flashypdfkit.ui.theme.ActivePalette
import com.flashypdfkit.ui.theme.AppRadius
import com.flashypdfkit.ui.theme.AppSpacing
import com.flashypdfkit.ui.theme.tactilePress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentScreen(
    recentFiles: List<ProcessedFile>,
    onBack: () -> Unit,
    onOpenFile: (ProcessedFile) -> Unit,
    onShareFile: (ProcessedFile) -> Unit,
    onDeleteFile: (ProcessedFile) -> Unit,
    onClearAll: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val canvasColor = if (isDark) ActivePalette.DarkCanvas else ActivePalette.LightCanvas
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val surfaceMuted = if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted

    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear History?",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove the file list records from your history. The actual exported files will remain in your storage.",
                    color = textMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActivePalette.Danger)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = textPrimary)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(AppRadius.lg)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
            .navigationBarsPadding()
    ) {
        ToolTopBar(
            title = "Processed History",
            onBack = onBack,
            trailing = {
                if (recentFiles.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.tactilePress { showClearDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear History",
                            tint = ActivePalette.Danger
                        )
                    }
                }
            }
        )

        if (recentFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(ActivePalette.Primary.copy(alpha = if (isDark) 0.15f else 0.08f))
                            .border(1.dp, ActivePalette.Primary.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = ActivePalette.Primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "No processed files yet",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Documents you merge, split, sign, compress, or convert will appear here for fast sharing.",
                        fontSize = 13.sp,
                        color = textMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(AppRadius.full),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ActivePalette.Primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ActivePalette.Primary),
                        modifier = Modifier.tactilePress { onBack() }
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse PDF Tools", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recentFiles, key = { it.id }) { file ->
                    val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(file.timestamp))
                    val sizeKb = (file.sizeBytes / 1024).coerceAtLeast(1)

                    val toolColor = when (file.toolType) {
                        PdfToolType.MERGE -> ActivePalette.Primary
                        PdfToolType.SPLIT -> ActivePalette.Honey
                        PdfToolType.COMPRESS -> ActivePalette.Teal
                        PdfToolType.PROTECT -> ActivePalette.Honey
                        PdfToolType.SIGN -> ActivePalette.Primary
                        PdfToolType.IMAGES_TO_PDF, PdfToolType.PDF_TO_IMAGES -> ActivePalette.Teal
                        else -> ActivePalette.Primary
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppRadius.lg))
                            .clickable { onOpenFile(file) }
                            .tactilePress { onOpenFile(file) },
                        shape = RoundedCornerShape(AppRadius.lg),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(AppRadius.sm))
                                    .background(toolColor.copy(alpha = if (isDark) 0.22f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = toolColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(AppRadius.full),
                                        color = toolColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = file.toolType.title,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = toolColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = "$sizeKb KB • $dateStr",
                                        fontSize = 10.sp,
                                        color = textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onShareFile(file) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .tactilePress { onShareFile(file) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = textMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDeleteFile(file) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .tactilePress { onDeleteFile(file) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = ActivePalette.Danger.copy(alpha = 0.8f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

