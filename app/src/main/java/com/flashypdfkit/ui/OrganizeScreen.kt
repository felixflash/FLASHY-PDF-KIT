package com.flashypdfkit.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.model.PageOrganizeItem
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

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.DragHandle

data class PageState(
    val item: PageOrganizeItem,
    val bitmap: Bitmap?,
    val id: String = java.util.UUID.randomUUID().toString()
)

@Composable
fun OrganizeScreen(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    onPickPdf: () -> Unit,
    onSaveResult: (File, PdfToolType) -> Unit,
    onShareResult: (File) -> Unit
) {
    val context = LocalContext.current
    val prefsHistory = remember { PreferencesAndHistory(context) }
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

    val canvasColor = if (isDark) ActivePalette.DarkCanvas else ActivePalette.LightCanvas
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val surfaceMuted = if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted

    var currentUri by remember { mutableStateOf(initialUri) }
    var isLoading by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<File?>(null) }
    val pageList = remember { mutableStateListOf<PageState>() }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val itemCoordinates = remember { mutableMapOf<Int, Rect>() }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            currentUri = initialUri
        }
    }

    LaunchedEffect(currentUri) {
        val uri = currentUri ?: return@LaunchedEffect
        isLoading = true
        pageList.clear()

        withContext(Dispatchers.IO) {
            val count = PdfEngine.getPdfPageCount(context, uri)
            for (i in 0 until count) {
                val bmp = PdfEngine.renderPdfPage(context, uri, i, 400)
                val item = PageOrganizeItem(originalIndex = i, pageNumber = i + 1, rotationDegrees = 0)
                withContext(Dispatchers.Main) {
                    pageList.add(PageState(item, bmp))
                }
            }
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        ToolTopBar(
            title = "Organize",
            onBack = onBack,
            infoTitle = "Rearrange & Orient Pages",
            infoText = "Rotate individual pages or delete unwanted ones before saving.",
            toolKey = "organize",
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
                    label = "Tap to choose a PDF to organize",
                    hint = "Select PDF document to edit pages",
                    onClick = onPickPdf
                )
            } else if (isLoading) {
                // Living loading state
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(AppRadius.xl),
                        color = surfaceColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(ActivePalette.Primary.copy(alpha = if (isDark) 0.25f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = ActivePalette.Primary,
                                    strokeWidth = 3.dp
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Preparing Document Pages…",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Rendering page thumbnails into your workspace.",
                                fontSize = 12.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pageList.size} Pages in Workspace",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(AppRadius.full),
                        color = if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = ActivePalette.Primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hold & drag to reorder",
                                fontSize = 11.sp,
                                color = textMuted
                            )
                        }
                    }
                }

                if (draggedIndex != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(AppRadius.full),
                        color = ActivePalette.Primary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ActivePalette.Primary.copy(alpha = 0.4f)),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Moving to position ${(draggedIndex ?: 0) + 1} of ${pageList.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ActivePalette.Primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(pageList, key = { _, state -> state.id }) { index, state ->
                        val isDragging = draggedIndex == index
                        Surface(
                            shape = RoundedCornerShape(AppRadius.lg),
                            color = if (isDragging) ActivePalette.Primary.copy(alpha = if (isDark) 0.25f else 0.12f) else surfaceColor,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isDragging) 2.dp else 1.dp,
                                if (isDragging) ActivePalette.Primary else borderColor
                            ),
                            modifier = Modifier
                                .animateItem()
                                .zIndex(if (isDragging) 10f else 1f)
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationX = dragOffset.x
                                        translationY = dragOffset.y
                                        scaleX = 1.06f
                                        scaleY = 1.06f
                                        shadowElevation = 18.dp.toPx()
                                    }
                                }
                                .onGloballyPositioned { coords ->
                                    if (draggedIndex != index) {
                                        itemCoordinates[index] = coords.boundsInRoot()
                                    }
                                }
                                .pointerInput(pageList.size, index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffset = Offset.Zero
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount

                                            val currentIndex = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                            val currentRect = itemCoordinates[currentIndex] ?: return@detectDragGesturesAfterLongPress
                                            val currentCenter = currentRect.center + dragOffset

                                            var closestIndex = currentIndex
                                            var minDistanceSq = Float.MAX_VALUE

                                            itemCoordinates.forEach { (slotIdx, rect) ->
                                                if (slotIdx in 0..pageList.lastIndex) {
                                                    val dx = rect.center.x - currentCenter.x
                                                    val dy = rect.center.y - currentCenter.y
                                                    val distSq = dx * dx + dy * dy
                                                    if (distSq < minDistanceSq) {
                                                        minDistanceSq = distSq
                                                        closestIndex = slotIdx
                                                    }
                                                }
                                            }

                                            if (closestIndex != currentIndex && closestIndex in 0..pageList.lastIndex) {
                                                val targetRect = itemCoordinates[closestIndex]
                                                if (targetRect != null) {
                                                    val movedItem = pageList.removeAt(currentIndex)
                                                    pageList.add(closestIndex, movedItem)
                                                    dragOffset = currentCenter - targetRect.center
                                                    draggedIndex = closestIndex
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffset = Offset.Zero
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Page preview canvas
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.75f)
                                        .clip(RoundedCornerShape(AppRadius.sm))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E6EC), RoundedCornerShape(AppRadius.sm)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.bitmap != null) {
                                        Image(
                                            bitmap = state.bitmap.asImageBitmap(),
                                            contentDescription = "Page ${state.item.pageNumber}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp)
                                                .rotate(state.item.rotationDegrees.toFloat())
                                        )
                                    }

                                    // Top page number badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Orig #${state.item.pageNumber}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    // Drag handle indicator badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.55f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Hold and drag to move",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    // Rotation badge if rotated
                                    if (state.item.rotationDegrees > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(ActivePalette.Primary.copy(alpha = 0.9f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${state.item.rotationDegrees}°",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Pos ${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDragging) ActivePalette.Primary else textPrimary
                                        )
                                        Text(
                                            text = if (isDragging) "Moving..." else "Page ${state.item.pageNumber}",
                                            fontSize = 10.sp,
                                            color = textMuted
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val currentRot = state.item.rotationDegrees
                                                val updatedItem = state.item.copy(rotationDegrees = (currentRot + 90) % 360)
                                                pageList[index] = state.copy(item = updatedItem)
                                            },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RotateRight,
                                                contentDescription = "Rotate 90 degrees",
                                                tint = ActivePalette.Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                pageList.removeAt(index)
                                            },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(ActivePalette.Danger.copy(alpha = if (isDark) 0.18f else 0.08f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Page",
                                                tint = ActivePalette.Danger,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (resultFile != null) {
                    ResultCard(
                        title = "PDF Organized Successfully!",
                        subtitle = "Saved new organized document to ${resultFile?.name}",
                        onSave = { resultFile?.let { onSaveResult(it, PdfToolType.ORGANIZE) } },
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
                            text = "Processes on-device in secure memory",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                    }

                    GradientButton(
                        text = "Save Organized PDF (${pageList.size} Pages)",
                        enabled = pageList.isNotEmpty(),
                        isLoading = isProcessing,
                        onClick = {
                            val uri = currentUri ?: return@GradientButton
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val items = pageList.map { it.item }
                                val outFile = File(context.cacheDir, "Organized_${System.currentTimeMillis()}.pdf")
                                val success = PdfEngine.organizePdf(context, uri, items, outFile)
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    if (success) {
                                        resultFile = outFile
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("organize_action_button")
                    )
                }
            }
        }
    }
}

