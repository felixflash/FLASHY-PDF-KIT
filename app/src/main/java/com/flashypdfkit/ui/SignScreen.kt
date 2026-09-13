package com.flashypdfkit.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

@Composable
fun SignScreen(
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

    var step by remember { mutableIntStateOf(1) } // 1: draw signature, 2: pick PDF, 3: place signature

    val completedStrokes = remember { mutableStateListOf<List<Offset>>() }
    val currentStroke = remember { mutableStateListOf<Offset>() }
    var signatureBmp by remember { mutableStateOf<Bitmap?>(null) }

    var pdfUri by remember { mutableStateOf(initialUri) }
    var pageBmp by remember { mutableStateOf<Bitmap?>(null) }

    var signOffsetX by remember { mutableFloatStateOf(40f) }
    var signOffsetY by remember { mutableFloatStateOf(200f) }
    var renderedPageW by remember { mutableFloatStateOf(1f) }
    var renderedPageH by remember { mutableFloatStateOf(1f) }
    var sigDisplayW by remember { mutableFloatStateOf(1f) }
    var sigDisplayH by remember { mutableFloatStateOf(1f) }
    var sigScale by remember { mutableFloatStateOf(0.35f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    var isProcessing by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            pdfUri = initialUri
        }
    }

    LaunchedEffect(pdfUri) {
        val uri = pdfUri ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val bmp = PdfEngine.renderPdfPage(context, uri, 0, 1080)
            withContext(Dispatchers.Main) {
                pageBmp = bmp
                if (signatureBmp != null) step = 3
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        ToolTopBar(
            title = when (step) {
                1 -> "Step 1: Sign"
                2 -> "Step 2: Choose PDF"
                else -> "Step 3: Place & Resize"
            },
            onBack = onBack,
            infoTitle = "Draw & Apply Signature",
            infoText = "Draw your signature with your finger, select a PDF, then drag and pinch-to-zoom to position and resize it exactly where you need it.",
            toolKey = "sign",
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
                val uses = com.flashypdfkit.ads.UsageManager.getSignUses(context)
                val extra = com.flashypdfkit.ads.UsageManager.getSignRewardedExtra(context)
                val remaining = (com.flashypdfkit.ads.UsageManager.MAX_SIGN_FREE - uses).coerceAtLeast(0) + extra
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

            // Step breadcrumbs card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.lg),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stepsList = listOf("1. Draw", "2. Choose", "3. Place")
                    stepsList.forEachIndexed { index, stepName ->
                        val stepNumber = index + 1
                        val isCurrent = step == stepNumber
                        val isDone = step > stepNumber

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCurrent -> ActivePalette.Primary
                                            isDone -> ActivePalette.Teal
                                            else -> borderColor
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isDone) "✓" else "$stepNumber",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent || isDone) Color.White else textMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stepName,
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) textPrimary else textMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (step == 1) {
                // Signature Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(AppRadius.xl))
                        .background(Color.White)
                        .border(2.dp, ActivePalette.Primary.copy(alpha = 0.5f), RoundedCornerShape(AppRadius.xl))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStroke.clear()
                                    currentStroke.add(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentStroke.add(change.position)
                                },
                                onDragEnd = {
                                    if (currentStroke.isNotEmpty()) {
                                        completedStrokes.add(currentStroke.toList())
                                    }
                                    currentStroke.clear()
                                },
                                onDragCancel = {
                                    currentStroke.clear()
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (stroke in completedStrokes) {
                            if (stroke.size == 1) {
                                drawCircle(color = Color(0xFF1E293B), radius = 3.5f, center = stroke[0])
                            } else if (stroke.size > 1) {
                                val strokePath = Path()
                                strokePath.moveTo(stroke[0].x, stroke[0].y)
                                for (i in 1 until stroke.size) {
                                    strokePath.lineTo(stroke[i].x, stroke[i].y)
                                }
                                drawPath(
                                    path = strokePath,
                                    color = Color(0xFF0F172A),
                                    style = Stroke(
                                        width = 6f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        if (currentStroke.size == 1) {
                            drawCircle(color = Color(0xFF1E293B), radius = 3.5f, center = currentStroke[0])
                        } else if (currentStroke.size > 1) {
                            val strokePath = Path()
                            strokePath.moveTo(currentStroke[0].x, currentStroke[0].y)
                            for (i in 1 until currentStroke.size) {
                                strokePath.lineTo(currentStroke[i].x, currentStroke[i].y)
                            }
                            drawPath(
                                path = strokePath,
                                color = Color(0xFF0F172A),
                                style = Stroke(
                                    width = 6f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (completedStrokes.isEmpty() && currentStroke.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Draw,
                                    contentDescription = null,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Sign here with your finger",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Toolbar for canvas actions (Undo & Clear)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (completedStrokes.isNotEmpty()) {
                                    completedStrokes.removeAt(completedStrokes.size - 1)
                                }
                            },
                            enabled = completedStrokes.isNotEmpty(),
                            shape = RoundedCornerShape(AppRadius.md),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                            modifier = Modifier.tactilePress(enabled = completedStrokes.isNotEmpty()) {
                                if (completedStrokes.isNotEmpty()) {
                                    completedStrokes.removeAt(completedStrokes.size - 1)
                                }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                modifier = Modifier.size(16.dp),
                                tint = if (completedStrokes.isNotEmpty()) textPrimary else textMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Undo", fontSize = 13.sp, color = if (completedStrokes.isNotEmpty()) textPrimary else textMuted)
                        }

                        OutlinedButton(
                            onClick = {
                                completedStrokes.clear()
                                currentStroke.clear()
                            },
                            enabled = completedStrokes.isNotEmpty() || currentStroke.isNotEmpty(),
                            shape = RoundedCornerShape(AppRadius.md),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (completedStrokes.isNotEmpty() || currentStroke.isNotEmpty()) ActivePalette.Danger.copy(alpha = 0.4f) else borderColor
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ActivePalette.Danger
                            ),
                            modifier = Modifier.tactilePress(enabled = completedStrokes.isNotEmpty() || currentStroke.isNotEmpty()) {
                                completedStrokes.clear()
                                currentStroke.clear()
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear",
                                tint = if (completedStrokes.isNotEmpty() || currentStroke.isNotEmpty()) ActivePalette.Danger else textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear", color = if (completedStrokes.isNotEmpty() || currentStroke.isNotEmpty()) ActivePalette.Danger else textMuted, fontSize = 13.sp)
                        }
                    }

                    if (completedStrokes.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(AppRadius.full),
                            color = surfaceMuted,
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Text(
                                text = "${completedStrokes.size} stroke${if (completedStrokes.size > 1) "s" else ""}",
                                fontSize = 11.sp,
                                color = textMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Primary full-width CTA
                GradientButton(
                    text = "Use Signature",
                    enabled = completedStrokes.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("use_signature_button"),
                    onClick = {
                        val allPoints = completedStrokes.flatten()
                        if (allPoints.isEmpty()) return@GradientButton

                        val minX = allPoints.minOf { it.x }
                        val maxX = allPoints.maxOf { it.x }
                        val minY = allPoints.minOf { it.y }
                        val maxY = allPoints.maxOf { it.y }

                        val rawW = (maxX - minX).coerceAtLeast(20f)
                        val rawH = (maxY - minY).coerceAtLeast(20f)
                        val pad = 24f
                        val targetW = (rawW + pad * 2).toInt()
                        val targetH = (rawH + pad * 2).toInt()

                        val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        canvas.drawColor(AndroidColor.TRANSPARENT)

                        val paint = AndroidPaint().apply {
                            color = AndroidColor.BLACK
                            strokeWidth = 8f
                            style = AndroidPaint.Style.STROKE
                            strokeCap = AndroidPaint.Cap.ROUND
                            strokeJoin = AndroidPaint.Join.ROUND
                            isAntiAlias = true
                        }

                        for (stroke in completedStrokes) {
                            if (stroke.size == 1) {
                                canvas.drawCircle(stroke[0].x - minX + pad, stroke[0].y - minY + pad, 4f, paint)
                            } else if (stroke.size > 1) {
                                val path = android.graphics.Path()
                                path.moveTo(stroke[0].x - minX + pad, stroke[0].y - minY + pad)
                                for (i in 1 until stroke.size) {
                                    path.lineTo(stroke[i].x - minX + pad, stroke[i].y - minY + pad)
                                }
                                canvas.drawPath(path, paint)
                            }
                        }

                        signatureBmp = bmp
                        if (pdfUri == null) step = 2 else step = 3
                    }
                )
            } else if (step == 2) {
                Text(
                    text = "Select the PDF document to stamp with your signature.",
                    fontSize = 13.sp,
                    color = textMuted,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Dropzone(
                    label = "Choose PDF Document",
                    hint = "Select PDF document to stamp signature",
                    onClick = onPickPdf
                )

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = { step = 1 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .tactilePress { step = 1 },
                    shape = RoundedCornerShape(AppRadius.lg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp), tint = ActivePalette.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("← Redraw Signature", color = textPrimary, fontSize = 14.sp)
                }
            } else if (step == 3) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(AppRadius.lg))
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current

                    if (pageBmp != null) {
                        val bmpW = pageBmp!!.width.toFloat()
                        val bmpH = pageBmp!!.height.toFloat()

                        val maxBoxW = constraints.maxWidth.toFloat()
                        val maxBoxH = constraints.maxHeight.toFloat()
                        val fitScale = minOf(maxBoxW / bmpW, maxBoxH / bmpH)

                        val currentRenderedPageW = bmpW * fitScale
                        val currentRenderedPageH = bmpH * fitScale
                        renderedPageW = currentRenderedPageW
                        renderedPageH = currentRenderedPageH

                        val renderedPageWDp = with(density) { currentRenderedPageW.toDp() }
                        val renderedPageHDp = with(density) { currentRenderedPageH.toDp() }

                        Box(
                            modifier = Modifier
                                .size(renderedPageWDp, renderedPageHDp)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFCBD5E1))
                                .pointerInput(Unit) {
                                    detectTransformGestures { centroid, pan, zoom, rotation ->
                                        signOffsetX += pan.x
                                        signOffsetY += pan.y

                                        if (zoom != 1f) {
                                            val oldScale = sigScale
                                            val newScale = (sigScale * zoom).coerceIn(0.08f, 1.30f)
                                            if (oldScale > 0f) {
                                                val ratio = newScale / oldScale
                                                signOffsetX = centroid.x + (signOffsetX - centroid.x) * ratio
                                                signOffsetY = centroid.y + (signOffsetY - centroid.y) * ratio
                                            }
                                            sigScale = newScale
                                        }

                                        if (kotlin.math.abs(rotation) > 0.05f) {
                                            rotationDegrees = (rotationDegrees + rotation) % 360f
                                        }
                                    }
                                }
                        ) {
                            Image(
                                bitmap = pageBmp!!.asImageBitmap(),
                                contentDescription = "PDF Page Preview",
                                modifier = Modifier.fillMaxSize()
                            )

                            if (signatureBmp != null) {
                                val sigBmpW = signatureBmp!!.width.toFloat()
                                val sigBmpH = signatureBmp!!.height.toFloat()
                                val sigAspect = sigBmpW / sigBmpH

                                val currentSigDisplayW = (currentRenderedPageW * sigScale).coerceIn(40f, currentRenderedPageW * 1.5f)
                                val currentSigDisplayH = (currentSigDisplayW / sigAspect).coerceIn(20f, currentRenderedPageH * 1.5f)
                                sigDisplayW = currentSigDisplayW
                                sigDisplayH = currentSigDisplayH

                                val sigDisplayWDp = with(density) { currentSigDisplayW.toDp() }
                                val sigDisplayHDp = with(density) { currentSigDisplayH.toDp() }

                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                signOffsetX.roundToInt(),
                                                signOffsetY.roundToInt()
                                            )
                                        }
                                        .size(sigDisplayWDp, sigDisplayHDp)
                                        .graphicsLayer {
                                            this.rotationZ = rotationDegrees
                                        }
                                        .border(1.5.dp, ActivePalette.Primary, RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                ) {
                                    Image(
                                        bitmap = signatureBmp!!.asImageBitmap(),
                                        contentDescription = "Signature Overlay",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                    )

                                    // Minimal, clean visual zoom level badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-10).dp)
                                            .background(ActivePalette.Secondary, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${(sigScale * 100).roundToInt()}%",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = ActivePalette.Primary)
                    }
                }

                // Comfortable signature controls card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    shape = RoundedCornerShape(AppRadius.lg),
                    color = surfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Size Label + [-] + Slider + [+] + Percentage Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Scale",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )

                            IconButton(
                                onClick = { sigScale = (sigScale - 0.05f).coerceIn(0.10f, 1.20f) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(surfaceMuted)
                                    .tactilePress { sigScale = (sigScale - 0.05f).coerceIn(0.10f, 1.20f) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Zoom Out",
                                    tint = ActivePalette.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Slider(
                                value = sigScale,
                                onValueChange = { sigScale = it },
                                valueRange = 0.10f..1.20f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = ActivePalette.Primary,
                                    activeTrackColor = ActivePalette.Primary,
                                    inactiveTrackColor = borderColor
                                )
                            )

                            IconButton(
                                onClick = { sigScale = (sigScale + 0.05f).coerceIn(0.10f, 1.20f) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(surfaceMuted)
                                    .tactilePress { sigScale = (sigScale + 0.05f).coerceIn(0.10f, 1.20f) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Zoom In",
                                    tint = ActivePalette.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "${(sigScale * 100).roundToInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ActivePalette.Primary,
                                modifier = Modifier.width(38.dp)
                            )
                        }

                        // Row 2: Rotation & Reset quick actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { rotationDegrees = (rotationDegrees - 90f) % 360f },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .tactilePress { rotationDegrees = (rotationDegrees - 90f) % 360f },
                                    shape = RoundedCornerShape(AppRadius.sm),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RotateLeft,
                                        contentDescription = "Rotate Left",
                                        tint = textMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("-90°", fontSize = 11.sp, color = textMuted)
                                }

                                OutlinedButton(
                                    onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .tactilePress { rotationDegrees = (rotationDegrees + 90f) % 360f },
                                    shape = RoundedCornerShape(AppRadius.sm),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RotateRight,
                                        contentDescription = "Rotate Right",
                                        tint = textMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+90°", fontSize = 11.sp, color = textMuted)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    sigScale = 0.35f
                                    rotationDegrees = 0f
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .tactilePress {
                                        sigScale = 0.35f
                                        rotationDegrees = 0f
                                    },
                                shape = RoundedCornerShape(AppRadius.sm),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = textMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", fontSize = 11.sp, color = textMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (resultFile != null) {
                    ResultCard(
                        title = "PDF Signed Successfully!",
                        subtitle = "Saved signed document to ${resultFile?.name}",
                        onSave = { resultFile?.let { onSaveResult(it, PdfToolType.SIGN) } },
                        onShare = { resultFile?.let { onShareResult(it) } }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .tactilePress { step = 1 },
                            shape = RoundedCornerShape(AppRadius.lg),
                            colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Text("← Resign", color = textPrimary, fontSize = 14.sp)
                        }

                        GradientButton(
                            text = "Save Signed PDF",
                            isLoading = isProcessing,
                            modifier = Modifier
                                .weight(2f)
                                .testTag("save_signed_pdf_button"),
                            onClick = {
                                val uri = pdfUri ?: return@GradientButton
                                val sigBmp = signatureBmp ?: return@GradientButton

                                isProcessing = true
                                scope.launch(Dispatchers.IO) {
                                    val safePageW = renderedPageW.coerceAtLeast(1f)
                                    val safePageH = renderedPageH.coerceAtLeast(1f)

                                    val normX = (signOffsetX / safePageW).coerceIn(0f, 0.95f)
                                    val normY = (signOffsetY / safePageH).coerceIn(0f, 0.95f)
                                    val normW = (sigDisplayW / safePageW).coerceIn(0.02f, 0.95f)
                                    val normH = (sigDisplayH / safePageH).coerceIn(0.02f, 0.95f)

                                    val outFile = File(context.cacheDir, "Signed_${System.currentTimeMillis()}.pdf")
                                    val success = PdfEngine.signPdf(
                                        context = context,
                                        pdfUri = uri,
                                        signatureBitmap = sigBmp,
                                        targetPageIndex = 0,
                                        normX = normX,
                                        normY = normY,
                                        normW = normW,
                                        normH = normH,
                                        rotationDegrees = rotationDegrees,
                                        outputFile = outFile
                                    )
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        if (success) {
                                            resultFile = outFile
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


