package com.flashypdfkit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.net.Uri
import android.widget.Toast
import com.flashypdfkit.model.PdfToolType
import com.flashypdfkit.ui.theme.ActivePalette
import com.flashypdfkit.ui.theme.AppRadius
import com.flashypdfkit.ui.theme.AppShapes
import com.flashypdfkit.ui.theme.AppSpacing
import com.flashypdfkit.ui.theme.isAppInDarkTheme
import com.flashypdfkit.ui.theme.tactilePress
import androidx.compose.foundation.clickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

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
    val lazyListState = rememberLazyListState()
    val isDark = isAppInDarkTheme()

    val isOnline by com.flashypdfkit.ads.NetworkMonitor.isOnline.collectAsState()
    var isAdLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        if (!isOnline) {
            isAdLoaded = false
        }
    }

    val canvasColor = if (isDark) ActivePalette.DarkCanvas else ActivePalette.LightCanvas
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted
    val borderStroke = remember(borderColor) { BorderStroke(1.dp, borderColor) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
        // --- 1. TOP BAR ---
        item(key = "top_bar") {
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
                            .clip(AppShapes.md)
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
                                        .clip(AppShapes.badge)
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
                            overflow = TextOverflow.Ellipsis
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
                                .clip(AppShapes.full)
                                .background(
                                    if (isDark) ActivePalette.PrimarySubtleDark
                                    else ActivePalette.PrimarySubtleLight
                                )
                                .border(
                                    1.dp,
                                    ActivePalette.Primary.copy(alpha = 0.35f),
                                    AppShapes.full
                                )
                                .clickable(onClick = onOpenUpgradeModal)
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
                            .clip(AppShapes.md)
                            .background(surfaceColor)
                            .border(1.dp, borderColor, AppShapes.md)
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
        }

        // --- 2. HERO CARD (PRIMARY ACTION) ---
        item(key = "hero_card") {
            Card(
                onClick = { onSelectTool(PdfToolType.READ) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs)
                    .testTag("hero_open_pdf_card"),
                shape = AppShapes.lg,
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = borderStroke
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(ActivePalette.Success)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "100% On-Device & Private",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ActivePalette.Teal
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Open & Read Any PDF",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Smooth page scrolling, pinch-to-zoom, and offline viewing.",
                                fontSize = 12.sp,
                                color = textMuted,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(AppShapes.md)
                                .background(ActivePalette.SunsetGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reassuring privacy pill bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.sm)
                            .background(
                                if (isDark) ActivePalette.DarkSurfaceMuted
                                else ActivePalette.LightSurfaceMuted
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ActivePalette.Navy,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Files never leave your phone. Instant, secure processing.",
                            fontSize = 10.5.sp,
                            color = textMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- 3. SECTION: EDIT & ORGANIZE ---
        item(key = "section_edit_header") {
            SectionHeader(
                title = "Edit & Organize",
                subtitle = "Combine, extract, shrink, and reorder document pages",
                textColor = textPrimary,
                mutedColor = textMuted
            )
        }

        item(key = "tool_row_merge_split") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VibrantToolCard(
                    title = "Merge",
                    subtitle = "Combine multiple PDFs into one document",
                    badge = "Fast",
                    icon = Icons.AutoMirrored.Filled.MergeType,
                    accentColor = ActivePalette.Primary,
                    isDark = isDark,
                    borderStroke = borderStroke,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTool(PdfToolType.MERGE) },
                    testTag = "tool_card_merge"
                )
                VibrantToolCard(
                    title = "Split",
                    subtitle = "Extract or divide pages into separate files",
                    badge = null,
                    icon = Icons.AutoMirrored.Filled.CallSplit,
                    accentColor = ActivePalette.Teal,
                    isDark = isDark,
                    borderStroke = borderStroke,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTool(PdfToolType.SPLIT) },
                    testTag = "tool_card_split"
                )
            }
        }

        item(key = "spacer_1") {
            Spacer(modifier = Modifier.height(14.dp))
        }

        item(key = "tool_row_compress_organize") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VibrantToolCard(
                    title = "Compress",
                    subtitle = "Reduce file size while preserving quality",
                    badge = "Save 65%",
                    icon = Icons.Default.Compress,
                    accentColor = ActivePalette.Honey,
                    isDark = isDark,
                    borderStroke = borderStroke,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTool(PdfToolType.COMPRESS) },
                    testTag = "tool_card_compress"
                )
                VibrantToolCard(
                    title = "Organize",
                    subtitle = "Reorder, rotate, or delete individual pages",
                    badge = null,
                    icon = Icons.Default.Layers,
                    accentColor = ActivePalette.Indigo,
                    isDark = isDark,
                    borderStroke = borderStroke,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTool(PdfToolType.ORGANIZE) },
                    testTag = "tool_card_organize"
                )
            }
        }

        // --- 4. SECTION: SIGN, SECURE & CONVERT ---
        item(key = "section_sign_header") {
            SectionHeader(
                title = "Sign, Secure & Convert",
                subtitle = "Draw e-signatures, AES-128 encryption, and image conversions",
                textColor = textPrimary,
                mutedColor = textMuted
            )
        }

        item(key = "tool_row_sign_lock") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VibrantToolCard(
                    title = "Sign",
                    subtitle = "Draw, insert, and place digital signatures",
                    badge = "Popular",
                    icon = Icons.Default.Edit,
                    accentColor = ActivePalette.Primary,
                    isDark = isDark,
                    borderStroke = borderStroke,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTool(PdfToolType.SIGN) },
                    testTag = "tool_card_sign"
                )
                VibrantToolCard(
                    title = "Lock",
                    subtitle = "Password-protect with AES encryption",
                    badge = "AES",
                    icon = Icons.Default.EnhancedEncryption,
                    accentColor = ActivePalette.Indigo,
                    isDark = isDark,
                    borderStroke = borderStroke,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTool(PdfToolType.PROTECT) },
                    testTag = "tool_card_protect"
                )
            }
        }

        item(key = "spacer_2") {
            Spacer(modifier = Modifier.height(14.dp))
        }

        item(key = "tool_row_images_convert") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VibrantToolCard(
                    title = "Images to PDF",
                    subtitle = "Turn photo scans into high-quality PDFs",
                    badge = null,
                    icon = Icons.Default.Image,
                    accentColor = ActivePalette.Honey,
                    isDark = isDark,
                    borderStroke = borderStroke,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTool(PdfToolType.IMAGES_TO_PDF) },
                    testTag = "tool_card_images_to_pdf"
                )
                VibrantToolCard(
                    title = "PDF to Images",
                    subtitle = "Export pages to clean PNG or JPEG images",
                    badge = null,
                    icon = Icons.Default.PictureInPicture,
                    accentColor = ActivePalette.Teal,
                    isDark = isDark,
                    borderStroke = borderStroke,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTool(PdfToolType.PDF_TO_IMAGES) },
                    testTag = "tool_card_pdf_to_images"
                )
            }
        }

        item(key = "spacer_3") {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recent Documents Card
        item(key = "recent_documents_card") {
            Surface(
                onClick = { onSelectTool(PdfToolType.RECENT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg)
                    .testTag("tool_card_recent"),
                shape = AppShapes.lg,
                color = surfaceColor,
                border = borderStroke
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(AppShapes.icon)
                                .background(ActivePalette.Teal.copy(alpha = if (isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = ActivePalette.Teal,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Recent Documents",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "View and resume previously opened PDFs",
                                fontSize = 11.5.sp,
                                color = textMuted,
                                maxLines = 1
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 6. GOOGLE DRIVE / WEB IMPORT CARD ---
        item(key = "spacer_4") {
            Spacer(modifier = Modifier.height(20.dp))
        }

        item(key = "drive_import_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.lg),
                shape = AppShapes.xl,
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) ActivePalette.DarkSurfaceMuted
                    else ActivePalette.LightSurfaceMuted
                ),
                border = borderStroke
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
                        textStyle = TextStyle(color = textPrimary, fontSize = 13.sp),
                        placeholder = {
                            Text(
                                "https://drive.google.com/file/...",
                                fontSize = 13.sp,
                                color = textMuted.copy(alpha = 0.7f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.md,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
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
                        shape = AppShapes.md,
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
        }

        // --- 7. FOOTER ---
        item(key = "footer") {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "FlashCom © 2026",
                fontSize = 12.sp,
                color = textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )
        }
    }

    // --- STICKY BOTTOM UNITY ADS BANNER (COLLAPSES COMPLETELY TO 0 HEIGHT WHEN NOT LOADED / OFFLINE) ---
    if (!isPremium && isOnline) {
        Box(
            modifier = if (isAdLoaded) {
                Modifier
                    .fillMaxWidth()
                    .background(surfaceColor)
                    .border(borderStroke)
                    .padding(vertical = 4.dp)
            } else {
                Modifier.size(0.dp)
            },
            contentAlignment = Alignment.Center
        ) {
            com.flashypdfkit.ads.UnityBannerView(
                modifier = if (isAdLoaded) Modifier.size(320.dp, 50.dp) else Modifier.size(0.dp),
                onAdLoaded = { isAdLoaded = true },
                onAdFailedToLoad = { isAdLoaded = false }
            )
        }
    }
}

    if (isDownloading) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = AppShapes.lg,
                color = surfaceColor,
                border = borderStroke
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
            .padding(start = AppSpacing.lg, end = AppSpacing.lg, top = 20.dp, bottom = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.5.sp,
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
    subtitle: String,
    badge: String?,
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    borderStroke: BorderStroke,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted
    val iconBgColor = remember(accentColor, isDark) {
        accentColor.copy(alpha = if (isDark) 0.22f else 0.12f)
    }
    val badgeBgColor = remember(accentColor, isDark) {
        accentColor.copy(alpha = if (isDark) 0.25f else 0.15f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag),
        shape = AppShapes.lg,
        color = surfaceColor,
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(AppShapes.icon)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(AppShapes.badge)
                            .background(badgeBgColor)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badge,
                            color = accentColor,
                            fontSize = 9.5.sp,
                            letterSpacing = (-0.1).sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(11.dp))

            Text(
                text = title,
                fontSize = 15.5.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.5.dp))

            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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

