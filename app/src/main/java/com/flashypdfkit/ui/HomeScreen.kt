package com.flashypdfkit.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import android.net.Uri
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import java.io.FileOutputStream
import java.io.IOException
import java.util.regex.Pattern
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.model.PdfToolType
import com.flashypdfkit.ui.theme.ActivePalette
import com.flashypdfkit.ui.theme.AppRadius
import com.flashypdfkit.ui.theme.AppSpacing
import com.flashypdfkit.ui.theme.tactilePress

@Composable
fun HomeScreen(
    isPremium: Boolean,
    onSelectTool: (PdfToolType) -> Unit,
    onOpenUpgradeModal: () -> Unit,
    onOpenSettings: () -> Unit,
    onFetchSuccess: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var driveUrl by remember { mutableStateOf("") }
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
    ) {
        // --- 1. TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(AppRadius.md))
                        .background(ActivePalette.SunsetGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FlashyPDF",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        if (isPremium) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ActivePalette.Honey)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = Color(0xFF2C1E05),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    Text(
                        text = "Vibrant, high-speed PDF studio",
                        fontSize = 12.sp,
                        color = textMuted,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Right Actions: Go Pro pill + Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                if (!isPremium) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppRadius.full))
                            .background(
                                if (isDark) ActivePalette.PrimarySubtleDark
                                else ActivePalette.PrimarySubtleLight
                            )
                            .border(
                                1.dp,
                                ActivePalette.Primary.copy(alpha = 0.35f),
                                RoundedCornerShape(AppRadius.full)
                            )
                            .tactilePress(onClick = onOpenUpgradeModal)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("home_upgrade_pill")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ActivePalette.Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Go Pro",
                                color = ActivePalette.Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(AppRadius.md))
                        .background(surfaceColor)
                        .border(1.dp, borderColor, RoundedCornerShape(AppRadius.md))
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 2. HERO CARD (PRIMARY ACTION) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
                .tactilePress(onClick = { onSelectTool(PdfToolType.READ) })
                .testTag("hero_open_pdf_card"),
            shape = RoundedCornerShape(AppRadius.xl),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ActivePalette.Success)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% On-Device & Private",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ActivePalette.Teal
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open & Read Any PDF",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Smooth page scrolling, pinch-to-zoom, and offline viewing with zero uploads.",
                            fontSize = 13.sp,
                            color = textMuted,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(AppRadius.lg))
                            .background(ActivePalette.SunsetGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reassuring privacy pill bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppRadius.md))
                        .background(
                            if (isDark) ActivePalette.DarkSurfaceMuted
                            else ActivePalette.LightSurfaceMuted
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ActivePalette.Navy,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Files never leave your phone. Instant, secure processing.",
                        fontSize = 11.sp,
                        color = textMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- 3. SECTION: ESSENTIALS ---
        SectionHeader(
            title = "Document Essentials",
            subtitle = "Core tools for everyday reading and shaping files",
            textColor = textPrimary,
            mutedColor = textMuted
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VibrantToolCard(
                title = "Merge",
                description = "Combine files into one clean document",
                badge = "Fast",
                icon = Icons.Default.MergeType,
                accentColor = ActivePalette.Primary,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTool(PdfToolType.MERGE) },
                testTag = "tool_card_merge"
            )
            VibrantToolCard(
                title = "Split",
                description = "Extract only the pages you need",
                badge = null,
                icon = Icons.Default.CallSplit,
                accentColor = ActivePalette.Teal,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTool(PdfToolType.SPLIT) },
                testTag = "tool_card_split"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VibrantToolCard(
                title = "Compress",
                description = "Shrink file size ~65% for easy email sharing",
                badge = "Save 65%",
                icon = Icons.Default.Compress,
                accentColor = ActivePalette.Honey,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTool(PdfToolType.COMPRESS) },
                testTag = "tool_card_compress"
            )
            VibrantToolCard(
                title = "Organize",
                description = "Reorder, delete, and rotate pages",
                badge = null,
                icon = Icons.Default.Layers,
                accentColor = ActivePalette.Indigo,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTool(PdfToolType.ORGANIZE) },
                testTag = "tool_card_organize"
            )
        }

        // --- 4. SECTION: SIGN & SECURE ---
        SectionHeader(
            title = "Sign & Secure",
            subtitle = "Add handwritten signatures and 128-bit encryption",
            textColor = textPrimary,
            mutedColor = textMuted
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VibrantToolCard(
                title = "Sign",
                description = "Draw signature or initials with exact placement",
                badge = "Popular",
                icon = Icons.Default.Edit,
                accentColor = ActivePalette.Primary,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTool(PdfToolType.SIGN) },
                testTag = "tool_card_sign"
            )
            VibrantToolCard(
                title = "Lock",
                description = "Protect with 128-bit standard encryption",
                badge = "AES",
                icon = Icons.Default.EnhancedEncryption,
                accentColor = ActivePalette.Indigo,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTool(PdfToolType.PROTECT) },
                testTag = "tool_card_protect"
            )
        }

        // --- 5. SECTION: CONVERT & HISTORY ---
        SectionHeader(
            title = "Convert & History",
            subtitle = "Transform formats and revisit recent files",
            textColor = textPrimary,
            mutedColor = textMuted
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VibrantToolCard(
                title = "Images to PDF",
                description = "Turn photos & scans into a single PDF",
                badge = null,
                icon = Icons.Default.Image,
                accentColor = ActivePalette.Honey,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTool(PdfToolType.IMAGES_TO_PDF) },
                testTag = "tool_card_images_to_pdf"
            )
            VibrantToolCard(
                title = "PDF to Images",
                description = "Export all pages as crisp JPG or PNG",
                badge = null,
                icon = Icons.Default.PictureInPicture,
                accentColor = ActivePalette.Teal,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTool(PdfToolType.PDF_TO_IMAGES) },
                testTag = "tool_card_pdf_to_images"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Full-width Recent Documents Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg)
                .tactilePress(onClick = { onSelectTool(PdfToolType.RECENT) })
                .testTag("tool_card_recent"),
            shape = RoundedCornerShape(AppRadius.lg),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(AppRadius.md))
                            .background(ActivePalette.Teal.copy(alpha = if (isDark) 0.2f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = ActivePalette.Teal,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Recent Documents",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "View, re-share, or export files you've worked on",
                            fontSize = 12.sp,
                            color = textMuted
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // --- 6. GOOGLE DRIVE / WEB IMPORT CARD ---
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg),
            shape = RoundedCornerShape(AppRadius.xl),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) ActivePalette.DarkSurfaceMuted
                else ActivePalette.LightSurfaceMuted
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = ActivePalette.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open from Web or Google Drive",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Paste a direct PDF link to import and start editing immediately.",
                    fontSize = 12.sp,
                    color = textMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = driveUrl,
                    onValueChange = { driveUrl = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    placeholder = {
                        Text(
                            "https://drive.google.com/file/...",
                            fontSize = 13.sp,
                            color = textMuted.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.md),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ActivePalette.Primary,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val trimmedUrl = driveUrl.trim()
                        if (trimmedUrl.isEmpty()) {
                            Toast.makeText(context, "Please enter a valid link first", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                isDownloading = true
                                var downloadedFile: File? = null
                                var errorMsg: String? = null
                                withContext(Dispatchers.IO) {
                                    try {
                                        downloadedFile = downloadPdfFile(context, trimmedUrl)
                                    } catch (e: Exception) {
                                        errorMsg = e.message ?: "Failed to download PDF"
                                    }
                                }
                                isDownloading = false
                                if (downloadedFile != null) {
                                    onFetchSuccess(Uri.fromFile(downloadedFile))
                                    driveUrl = ""
                                } else {
                                    Toast.makeText(context, errorMsg ?: "Connection error. Make sure the URL is public and direct.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(AppRadius.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActivePalette.Primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Fetch PDF Document",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (isDownloading) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(AppRadius.lg),
                    color = surfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = ActivePalette.Primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Downloading PDF Document...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fetching from link. Please wait...",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                    }
                }
            }
        }

        // --- UNITY ADS BANNER ---
        if (!isPremium) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                com.flashypdfkit.ads.UnityBannerView(
                    modifier = Modifier.size(320.dp, 50.dp)
                )
            }
        }

        // --- 7. FOOTER ---
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "FlashCom © 2026",
            fontSize = 12.sp,
            color = textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    textColor: Color,
    mutedColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = AppSpacing.lg, end = AppSpacing.lg, top = 26.dp, bottom = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = (-0.1).sp
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = mutedColor,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun VibrantToolCard(
    title: String,
    description: String,
    badge: String?,
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadius.md))
            .tactilePress(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(AppRadius.md),
        color = surfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = if (isDark) 0.25f else 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Download PDF Helper function with Redirect Following and Google Drive/Docs parsing support
private fun downloadPdfFile(context: android.content.Context, urlString: String): File {
    var finalUrl = urlString.trim()
    
    // Auto-detect and parse Google Drive links
    if (finalUrl.contains("drive.google.com")) {
        val fileId = extractGoogleDriveFileId(finalUrl)
        if (fileId != null) {
            finalUrl = "https://docs.google.com/uc?export=download&id=$fileId&confirm=t"
        }
    } else if (finalUrl.contains("docs.google.com/document/d/")) {
        val regex = java.util.regex.Pattern.compile("/document/d/([a-zA-Z0-9_-]+)")
        val matcher = regex.matcher(finalUrl)
        if (matcher.find()) {
            val docId = matcher.group(1)
            finalUrl = "https://docs.google.com/document/d/$docId/export?format=pdf"
        }
    } else if (finalUrl.contains("docs.google.com/spreadsheets/d/")) {
        val regex = java.util.regex.Pattern.compile("/spreadsheets/d/([a-zA-Z0-9_-]+)")
        val matcher = regex.matcher(finalUrl)
        if (matcher.find()) {
            val sheetId = matcher.group(1)
            finalUrl = "https://docs.google.com/spreadsheets/d/$sheetId/export?format=pdf"
        }
    }
    
    val url = java.net.URL(finalUrl)
    var conn = url.openConnection() as java.net.HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 15000
    conn.readTimeout = 15000
    conn.instanceFollowRedirects = true
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
    
    var status = conn.responseCode
    var redirectCount = 0
    while (status == java.net.HttpURLConnection.HTTP_MOVED_TEMP || 
           status == java.net.HttpURLConnection.HTTP_MOVED_PERM || 
           status == 307 || status == 308) {
        if (redirectCount > 5) break
        val newUrl = conn.getHeaderField("Location") ?: break
        conn.disconnect()
        val nextUrl = java.net.URL(newUrl)
        conn = nextUrl.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        status = conn.responseCode
        redirectCount++
    }
    
    if (status == 401 || status == 403) {
        throw java.io.IOException("This file isn't publicly accessible, please check sharing settings")
    }
    
    val contentType = conn.contentType ?: ""
    if (contentType.contains("text/html")) {
        throw java.io.IOException("This file isn't publicly accessible, please check sharing settings")
    }

    if (status != java.net.HttpURLConnection.HTTP_OK) {
        throw java.io.IOException("Server returned status: $status")
    }
    
    val cacheDir = context.cacheDir
    val tempFile = File.createTempFile("fetched_", ".pdf", cacheDir)
    
    conn.inputStream.use { inputStream ->
        java.io.FileOutputStream(tempFile).use { outputStream ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }
    }
    conn.disconnect()
    
    if (tempFile.length() < 10) {
        tempFile.delete()
        throw java.io.IOException("Downloaded file is empty or invalid")
    }
    
    return tempFile
}

private fun extractGoogleDriveFileId(url: String): String? {
    val patterns = listOf(
        "/file/d/([a-zA-Z0-9_-]+)",
        "id=([a-zA-Z0-9_-]+)"
    )
    for (pattern in patterns) {
        val regex = java.util.regex.Pattern.compile(pattern)
        val matcher = regex.matcher(url)
        if (matcher.find()) {
            return matcher.group(1)
        }
    }
    return null
}

