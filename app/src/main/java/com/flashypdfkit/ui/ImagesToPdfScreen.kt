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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.data.PreferencesAndHistory
import com.flashypdfkit.model.PdfToolType
import com.flashypdfkit.pdf.PdfEngine
import com.flashypdfkit.ui.components.Dropzone
import com.flashypdfkit.ui.components.FileItemCard
import com.flashypdfkit.ui.components.GradientButton
import com.flashypdfkit.ui.components.ImageFileThumbnail
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
fun ImagesToPdfScreen(
    selectedImages: List<Uri>,
    onBack: () -> Unit,
    onPickImages: () -> Unit,
    onTakeCameraPhoto: () -> Unit,
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

    val imageList = remember { mutableStateListOf<Uri>().apply { addAll(selectedImages) } }
    var isProcessing by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(selectedImages.toList()) {
        if (selectedImages.isNotEmpty()) {
            imageList.clear()
            imageList.addAll(selectedImages)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        ToolTopBar(
            title = "Images to PDF",
            onBack = onBack,
            infoTitle = "Photo to Document Converter",
            infoText = "Turn photos, receipts, and scans into a single high-quality PDF document.",
            toolKey = "images_to_pdf",
            prefsHistory = prefsHistory
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
        ) {

            // Action row: Select from Gallery or Scan with Camera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(AppRadius.lg))
                        .tactilePress { onPickImages() },
                    shape = RoundedCornerShape(AppRadius.lg),
                    color = surfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = ActivePalette.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gallery Photos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(AppRadius.lg))
                        .tactilePress { onTakeCameraPhoto() },
                    shape = RoundedCornerShape(AppRadius.lg),
                    color = ActivePalette.Primary.copy(alpha = if (isDark) 0.18f else 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ActivePalette.Primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = ActivePalette.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Camera",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ActivePalette.Primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (imageList.isEmpty()) {
                Dropzone(
                    label = "Tap to choose images",
                    hint = "Supports JPG, PNG, WEBP and Camera capture",
                    onClick = onPickImages
                )
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${imageList.size} Pages in PDF",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Clear all",
                        fontSize = 12.sp,
                        color = ActivePalette.Danger,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppRadius.sm))
                            .tactilePress { imageList.clear(); resultFile = null }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(imageList) { index, uri ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppRadius.md),
                            color = surfaceColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ImageFileThumbnail(
                                    uri = uri,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(AppRadius.sm))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uri.lastPathSegment ?: "Image ${index + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Page ${index + 1} of ${imageList.size}",
                                        fontSize = 11.sp,
                                        color = textMuted
                                    )
                                }

                                IconButton(
                                    onClick = { imageList.removeAt(index) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .tactilePress { imageList.removeAt(index) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove page",
                                        tint = ActivePalette.Danger,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (resultFile != null) {
                ResultCard(
                    title = "PDF Created Successfully!",
                    subtitle = "Converted ${imageList.size} images into ${resultFile?.name}",
                    onSave = { resultFile?.let { onSaveResult(it, PdfToolType.IMAGES_TO_PDF) } },
                    onShare = { resultFile?.let { onShareResult(it) } }
                )
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
                        text = "100% on-device image rendering & packaging",
                        fontSize = 11.sp,
                        color = textMuted
                    )
                }

                GradientButton(
                    text = if (imageList.isEmpty()) "Select Photos to Begin" else "Create PDF from ${imageList.size} Photos",
                    enabled = imageList.isNotEmpty(),
                    isLoading = isProcessing,
                    onClick = {
                        isProcessing = true
                        scope.launch(Dispatchers.IO) {
                            val outFile = File(context.cacheDir, "Images_${System.currentTimeMillis()}.pdf")
                            val success = PdfEngine.imagesToPdf(context, imageList, outFile)
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                if (success) {
                                    resultFile = outFile
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("create_pdf_button")
                )
            }
        }
    }
}

