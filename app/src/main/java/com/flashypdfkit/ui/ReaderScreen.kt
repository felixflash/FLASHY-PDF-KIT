package com.flashypdfkit.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import java.io.File
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.pdf.PdfEngine
import com.flashypdfkit.ui.theme.ActivePalette
import com.flashypdfkit.ui.theme.AppRadius
import com.flashypdfkit.ui.theme.AppSpacing
import com.flashypdfkit.ui.theme.tactilePress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReaderScreen(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    onPickPdf: () -> Unit,
    onSharePdf: (Uri) -> Unit,
    onSavePdf: (Uri) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val canvasColor = if (isDark) ActivePalette.DarkCanvas else Color(0xFFF2F4F8)
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted

    var currentUri by remember { mutableStateOf(initialUri) }
    var decryptedUri by remember { mutableStateOf<Uri?>(null) }
    var showPasswordPrompt by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var tempFileToDelete by remember { mutableStateOf<File?>(null) }

    var fullscreenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fullscreenPageIndex by remember { mutableStateOf<Int?>(null) }

    var pageCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    val renderedPages = remember { mutableStateListOf<Bitmap?>() }
    val listState = rememberLazyListState()

    val firstVisibleIndex by remember {
        derivedStateOf {
            if (pageCount > 0) (listState.firstVisibleItemIndex + 1).coerceAtMost(pageCount) else 0
        }
    }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            currentUri = initialUri
        }
    }

    // Clean up temporary file on exit/dispose or when currentUri changes
    DisposableEffect(currentUri) {
        onDispose {
            tempFileToDelete?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        }
    }

    LaunchedEffect(currentUri) {
        val uri = currentUri ?: return@LaunchedEffect
        isLoading = true
        renderedPages.clear()
        pageCount = 0
        decryptedUri = null
        showPasswordPrompt = false
        passwordError = null

        val isEncrypted = withContext(Dispatchers.IO) {
            PdfEngine.isPdfEncrypted(context, uri)
        }

        if (isEncrypted) {
            showPasswordPrompt = true
            isLoading = false
        } else {
            decryptedUri = uri
        }
    }

    LaunchedEffect(decryptedUri) {
        val uri = decryptedUri ?: return@LaunchedEffect
        isLoading = true
        renderedPages.clear()

        withContext(Dispatchers.IO) {
            val count = PdfEngine.getPdfPageCount(context, uri)
            pageCount = count
            val targetWidth = (1080 * zoomScale).toInt()

            for (i in 0 until count) {
                val bmp = PdfEngine.renderPdfPage(context, uri, i, targetWidth)
                withContext(Dispatchers.Main) {
                    renderedPages.add(bmp)
                }
            }
        }
        isLoading = false
    }

    // When zoom scale changes, reload high-res if needed
    LaunchedEffect(zoomScale) {
        val uri = decryptedUri ?: return@LaunchedEffect
        if (renderedPages.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val targetWidth = (1080 * zoomScale).toInt()
                for (i in 0 until pageCount) {
                    val bmp = PdfEngine.renderPdfPage(context, uri, i, targetWidth)
                    withContext(Dispatchers.Main) {
                        if (i < renderedPages.size) {
                            renderedPages[i] = bmp
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- TOP TOOLBAR ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = surfaceColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back button & Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(AppRadius.md))
                                .background(if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted)
                                .border(1.dp, borderColor, RoundedCornerShape(AppRadius.md))
                                .testTag("reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (currentUri != null) {
                                    currentUri?.lastPathSegment?.substringAfterLast("/") ?: "Document"
                                } else "PDF Reader",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (currentUri != null && pageCount > 0) {
                                    "Viewing page $firstVisibleIndex of $pageCount"
                                } else "Immersive document view",
                                fontSize = 12.sp,
                                color = textMuted
                            )
                        }
                    }

                    // Top trailing action buttons (when document is loaded)
                    if (currentUri != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pick another document button
                            IconButton(
                                onClick = onPickPdf,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(AppRadius.md))
                                    .background(if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted)
                                    .testTag("reader_change_file_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Open Another",
                                    tint = textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Share button
                            IconButton(
                                onClick = { currentUri?.let { onSharePdf(it) } },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(AppRadius.md))
                                    .background(if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted)
                                    .testTag("reader_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share PDF",
                                    tint = ActivePalette.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Save to Downloads button
                            IconButton(
                                onClick = { currentUri?.let { onSavePdf(it) } },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(AppRadius.md))
                                    .background(ActivePalette.Primary.copy(alpha = 0.12f))
                                    .testTag("reader_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Save Copy",
                                    tint = ActivePalette.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- MAIN BODY CONTENT ---
            if (currentUri == null) {
                // Friendly Warm Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.xl),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(AppRadius.lg))
                                    .background(ActivePalette.SunsetGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Ready to Read Your Document",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Select any PDF from your device to view with smooth scrolling, crisp zooming, and complete offline privacy.",
                                fontSize = 13.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Benefit badges
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(AppRadius.md))
                                    .background(if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ActivePalette.Teal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "High-definition vector page rendering",
                                        fontSize = 12.sp,
                                        color = textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ActivePalette.Teal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Continuous vertical flow & zoom controls",
                                        fontSize = 12.sp,
                                        color = textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = ActivePalette.Navy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Processed strictly on-device without cloud sync",
                                        fontSize = 12.sp,
                                        color = textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onPickPdf,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .tactilePress(onClick = onPickPdf)
                                    .testTag("reader_choose_pdf_button"),
                                shape = RoundedCornerShape(AppRadius.md),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ActivePalette.Primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Choose PDF Document",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else if (isLoading && renderedPages.isEmpty()) {
                // Human, Living Loading State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        shape = RoundedCornerShape(AppRadius.xl),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = ActivePalette.Primary,
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Opening Your Document…",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rendering pages for crystal-clear reading",
                                fontSize = 12.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Rendered Document Pages in Paper Canvas
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState)
                ) {
                    itemsIndexed(renderedPages) { index, bitmap ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(zoomScale.coerceAtMost(1f))
                                .shadow(6.dp, RoundedCornerShape(AppRadius.md), spotColor = Color.Black.copy(alpha = 0.15f))
                                .border(1.dp, if (isDark) ActivePalette.DarkBorder else Color(0xFFE2E6EC), RoundedCornerShape(AppRadius.md))
                                .clickable {
                                    if (bitmap != null) {
                                        fullscreenBitmap = bitmap
                                        fullscreenPageIndex = index
                                    }
                                }
                                .testTag("pdf_page_card_$index"),
                            shape = RoundedCornerShape(AppRadius.md),
                            color = Color.White
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(340.dp)
                                            .background(Color(0xFFFAFAFA)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = ActivePalette.Primary,
                                            modifier = Modifier.size(32.dp),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }

                                // Subtle, clean page divider bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF7F8FA))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Page ${index + 1} of $pageCount",
                                        color = Color(0xFF6B7A99),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // --- FLOATING TACTILE CONTROLS PILL (WHEN READING) ---
        AnimatedVisibility(
            visible = currentUri != null && pageCount > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(AppRadius.full), spotColor = Color.Black.copy(alpha = 0.25f))
                    .border(
                        1.dp,
                        if (isDark) ActivePalette.DarkBorder else Color.White.copy(alpha = 0.8f),
                        RoundedCornerShape(AppRadius.full)
                    ),
                shape = RoundedCornerShape(AppRadius.full),
                color = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page indicator badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppRadius.full))
                            .background(ActivePalette.Navy.copy(alpha = if (isDark) 0.35f else 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$firstVisibleIndex / $pageCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Zoom Out
                    IconButton(
                        onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.6f) },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .testTag("reader_zoom_out")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Current zoom percentage label
                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActivePalette.Primary,
                        modifier = Modifier.width(42.dp),
                        textAlign = TextAlign.Center
                    )

                    // Zoom In
                    IconButton(
                        onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(2.0f) },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .testTag("reader_zoom_in")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showPasswordPrompt) {
        var passwordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showPasswordPrompt = false
                onBack()
            },
            title = {
                Text(
                    text = "Password Protected PDF",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "This document is encrypted. Please enter the password to open it:",
                        color = textMuted,
                        modifier = Modifier.padding(bottom = AppSpacing.sm)
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("pdf_password_field"),
                        isError = passwordError != null
                    )
                    if (passwordError != null) {
                        Text(
                            text = passwordError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.testTag("pdf_password_unlock"),
                    onClick = {
                        isLoading = true
                        val uri = currentUri
                        if (uri != null) {
                            val tempFile = File(context.cacheDir, "decrypted_${System.currentTimeMillis()}.pdf")
                            val success = PdfEngine.removePdfProtection(context, uri, passwordInput, tempFile)
                            if (success) {
                                tempFileToDelete?.delete()
                                tempFileToDelete = tempFile
                                decryptedUri = Uri.fromFile(tempFile)
                                showPasswordPrompt = false
                                passwordError = null
                            } else {
                                passwordError = "Incorrect password or decryption error."
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordPrompt = false
                        onBack()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    fullscreenBitmap?.let { _ ->
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val scrollState = rememberLazyListState()

        // Pager state and scale state
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
            initialPage = fullscreenPageIndex ?: 0,
            pageCount = { renderedPages.size }
        )
        val coroutineScope = rememberCoroutineScope()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("pdf_fullscreen_overlay"),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            if (scale > 1f) {
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-(size.width * (scale - 1) / 2), (size.width * (scale - 1) / 2)),
                                    y = (offset.y + pan.y).coerceIn(-(size.height * (scale - 1) / 2), (size.height * (scale - 1) / 2))
                                )
                            } else {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        }
                    }
            ) { pageIndex ->
                renderedPages[pageIndex]?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF Page ${pageIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .clickable {
                                fullscreenBitmap = null
                                fullscreenPageIndex = null
                            }
                    )
                }
            }

            // Navigation Arrows
            if (scale == 1f) {
                // Previous
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Previous", tint = Color.White)
                    }
                }
                
                // Next
                if (pagerState.currentPage < renderedPages.size - 1) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
                    }
                }
            }

            
            // ... Close button and page label (kept same)

            // Close button at top-right
            IconButton(
                onClick = {
                    fullscreenBitmap = null
                    fullscreenPageIndex = null
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("pdf_fullscreen_close")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Fullscreen",
                    tint = Color.White
                )
            }

            // Page label and tips at the bottom
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(AppRadius.md)
            ) {
                Text(
                    text = "Page ${pagerState.currentPage + 1} of $pageCount (Pinch to zoom, drag to pan, tap to exit)",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
