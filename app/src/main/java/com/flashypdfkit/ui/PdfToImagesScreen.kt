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
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun PdfToImagesScreen(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    onPickPdf: () -> Unit,
    onSaveResult: (File, PdfToolType) -> Unit,
    onShareResult: (File) -> Unit,
    onSaveMultipleResults: ((List<File>) -> Unit)? = null,
    onShareMultipleResults: ((List<File>) -> Unit)? = null
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
    var selectedFormat by remember { mutableStateOf("jpg") }
    var scaleFactor by remember { mutableFloatStateOf(1.5f) }
    var isProcessing by remember { mutableStateOf(false) }
    var exportedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pageCount by remember { mutableStateOf(0) }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            currentUri = initialUri
        }
    }

    LaunchedEffect(currentUri) {
        val uri = currentUri
        if (uri != null) {
            withContext(Dispatchers.IO) {
                val count = PdfEngine.getPdfPageCount(context, uri)
                withContext(Dispatchers.Main) {
                    pageCount = count
                    errorMessage = null
                    exportedFiles = emptyList()
                }
            }
        } else {
            pageCount = 0
            exportedFiles = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        ToolTopBar(
            title = "PDF to Images",
            onBack = onBack,
            infoTitle = "Page Image Extraction",
            infoText = "Export all document pages as crisp, independent JPG or PNG high-resolution images.",
            toolKey = "pdf_to_images",
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
                    label = "Tap to choose a PDF to extract",
                    hint = "Select document to export pages as images",
                    onClick = onPickPdf
                )
            } else {
                FileItemCard(
                    name = currentUri?.lastPathSegment ?: "Selected Document",
                    sizeText = if (pageCount > 0) "$pageCount pages • Ready to extract" else "Ready to extract images",
                    uri = currentUri,
                    onRemove = { currentUri = null; exportedFiles = emptyList(); errorMessage = null }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Horizontal Page Preview
                Text(
                    text = "Document Preview ($pageCount Pages)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pageCount) { idx ->
                        Box(
                            modifier = Modifier
                                .size(width = 90.dp, height = 120.dp)
                                .clip(RoundedCornerShape(AppRadius.sm))
                                .border(1.dp, borderColor, RoundedCornerShape(AppRadius.sm))
                        ) {
                            com.flashypdfkit.ui.components.FilePreviewThumbnail(
                                context = context,
                                uri = currentUri!!,
                                pageIndex = idx,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f)
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

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.xl),
                    color = surfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Output Image Format",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val formats = listOf(
                                Pair("jpg", "JPG (Compact & Web)"),
                                Pair("png", "PNG (Lossless Crystal)")
                            )

                            formats.forEach { (fmt, label) ->
                                val isSelected = selectedFormat == fmt
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(AppRadius.md))
                                        .tactilePress { selectedFormat = fmt },
                                    shape = RoundedCornerShape(AppRadius.md),
                                    color = if (isSelected) ActivePalette.Primary.copy(alpha = if (isDark) 0.22f else 0.12f) else surfaceMuted,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) ActivePalette.Primary else borderColor
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = fmt.uppercase(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ActivePalette.Primary else textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (fmt == "jpg") "Compact file size" else "Crisp lossless",
                                            fontSize = 10.sp,
                                            color = if (isSelected) ActivePalette.Primary.copy(alpha = 0.85f) else textMuted
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Render Resolution",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(AppRadius.full),
                                color = ActivePalette.Teal.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${scaleFactor}x Quality",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ActivePalette.Teal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = scaleFactor,
                            onValueChange = { scaleFactor = it },
                            valueRange = 1f..3f,
                            steps = 3,
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
                            Text(text = "1.0x (Standard)", fontSize = 10.sp, color = textMuted)
                            Text(text = "1.5x (Balanced)", fontSize = 10.sp, color = textMuted)
                            Text(text = "3.0x (Ultra HD)", fontSize = 10.sp, color = textMuted)
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.md),
                        color = ActivePalette.Danger.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ActivePalette.Danger.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ActivePalette.Danger,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.sp,
                                color = ActivePalette.Danger,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (exportedFiles.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.xl),
                        color = surfaceColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ActivePalette.Teal,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${exportedFiles.size} Pages Extracted!",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Saved in temporary storage • ${selectedFormat.uppercase()}",
                                        fontSize = 11.sp,
                                        color = textMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Gallery preview of extracted image pages
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(exportedFiles) { idx, file ->
                                    val bmp = remember(file.absolutePath) {
                                        try {
                                            BitmapFactory.decodeFile(file.absolutePath)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(AppRadius.md),
                                        color = surfaceMuted,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                        modifier = Modifier.width(90.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp)
                                                    .clip(RoundedCornerShape(AppRadius.sm))
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (bmp != null) {
                                                    Image(
                                                        bitmap = bmp.asImageBitmap(),
                                                        contentDescription = "Page ${idx + 1}",
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Page ${idx + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (onSaveMultipleResults != null) {
                                            onSaveMultipleResults(exportedFiles)
                                        } else {
                                            onSaveResult(exportedFiles.first(), PdfToolType.PDF_TO_IMAGES)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("save_all_images_button"),
                                    shape = RoundedCornerShape(AppRadius.md),
                                    colors = ButtonDefaults.buttonColors(containerColor = ActivePalette.Teal)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Save All (${exportedFiles.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (onShareMultipleResults != null) {
                                            onShareMultipleResults(exportedFiles)
                                        } else {
                                            onShareResult(exportedFiles.first())
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("share_all_images_button"),
                                    shape = RoundedCornerShape(AppRadius.md),
                                    colors = ButtonDefaults.buttonColors(containerColor = ActivePalette.Primary)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Share Images",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
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
                            text = "Rendered 100% on device with hardware acceleration",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                    }

                    GradientButton(
                        text = if (pageCount > 0) "Extract $pageCount Pages as Images" else "Extract All Pages as Images",
                        isLoading = isProcessing,
                        onClick = {
                            val uri = currentUri ?: return@GradientButton
                            isProcessing = true
                            errorMessage = null
                            scope.launch(Dispatchers.IO) {
                                val outDir = File(context.cacheDir, "Exported_${System.currentTimeMillis()}").apply { mkdirs() }
                                val files = PdfEngine.pdfToImages(context, uri, selectedFormat, scaleFactor, outDir)
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    if (files.isEmpty()) {
                                        errorMessage = "Failed to extract images. The PDF may be password protected or corrupted."
                                    } else {
                                        exportedFiles = files
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("convert_to_images_button")
                    )
                }
            }
        }
    }
}

