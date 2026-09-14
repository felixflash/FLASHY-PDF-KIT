package com.flashypdfkit.ui

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.model.AppTheme
import com.flashypdfkit.ui.components.ToolTopBar
import com.flashypdfkit.ui.theme.ActivePalette
import com.flashypdfkit.ui.theme.AppRadius
import com.flashypdfkit.ui.theme.AppSpacing
import com.flashypdfkit.ui.theme.isAppInDarkTheme
import com.flashypdfkit.ui.theme.tactilePress

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    isPremium: Boolean,
    onBack: () -> Unit,
    onToggleTheme: (AppTheme) -> Unit,
    onOpenUpgrade: () -> Unit,
    onOpenLegal: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val canvasColor = if (isDark) ActivePalette.DarkCanvas else ActivePalette.LightCanvas
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val surfaceMuted = if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        ToolTopBar(title = "Settings", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
        ) {
            // PRO Banner if not premium
            if (!isPremium) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppRadius.xl))
                        .clickable { onOpenUpgrade() }
                        .tactilePress { onOpenUpgrade() }
                        .testTag("pro_upgrade_banner"),
                    shape = RoundedCornerShape(AppRadius.xl),
                    color = ActivePalette.Honey.copy(alpha = if (isDark) 0.16f else 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ActivePalette.Honey.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(AppRadius.sm))
                                .background(ActivePalette.Honey.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = ActivePalette.Honey,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FlashyPDF PRO",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(AppRadius.full),
                                    color = ActivePalette.Honey
                                ) {
                                    Text(
                                        text = "UPGRADE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Unlimited document processing & ad-free speed",
                                fontSize = 11.sp,
                                color = textMuted,
                                lineHeight = 15.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = ActivePalette.Honey,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
            }

            // APPEARANCE SECTION
            Text(
                text = "APPEARANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(AppRadius.lg),
                color = surfaceColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ActivePalette.Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentTheme == AppTheme.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = ActivePalette.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dark Mode",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = if (currentTheme == AppTheme.DARK) "Night-time contrast" else "Crisp paper aesthetic",
                                fontSize = 11.sp,
                                color = textMuted
                            )
                        }
                    }

                    Switch(
                        checked = currentTheme == AppTheme.DARK,
                        onCheckedChange = { isDarkTheme ->
                            onToggleTheme(if (isDarkTheme) AppTheme.DARK else AppTheme.LIGHT)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ActivePalette.Primary,
                            uncheckedThumbColor = textMuted,
                            uncheckedTrackColor = surfaceMuted
                        ),
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PRIVACY & ARCHITECTURE SECTION
            Text(
                text = "PRIVACY & SECURITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.lg),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(AppRadius.sm))
                            .background(ActivePalette.Teal.copy(alpha = if (isDark) 0.22f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ActivePalette.Teal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% Offline Processing",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Zero cloud uploads. All PDF rendering, merging, and cryptographic signing occurs locally in private sandbox storage.",
                            fontSize = 11.sp,
                            color = textMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ABOUT & LEGAL SECTION
            Text(
                text = "ABOUT & LEGAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            SettingsItem(
                icon = Icons.Default.Policy,
                title = "Privacy Policy & Terms",
                onClick = onOpenLegal
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Default.Info,
                title = "App Version",
                trailingText = "v2.5.0 (Build 250)"
            )

            Spacer(modifier = Modifier.height(8.dp))

            val context = LocalContext.current
            SettingsItem(
                icon = Icons.Default.Share,
                title = "Share App",
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "FlashyPDF")
                        putExtra(Intent.EXTRA_TEXT, "Check out FlashyPDF for offline PDF editing! https://play.google.com/store/apps/details?id=${context.packageName}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share app via"))
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Craft signature
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FlashyPDF Kit",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Crafted with Jetpack Compose & PDFBox Android",
                    fontSize = 11.sp,
                    color = textMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.lg))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .then(if (onClick != null) Modifier.tactilePress { onClick.invoke() } else Modifier),
        shape = RoundedCornerShape(AppRadius.lg),
        color = surfaceColor,
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
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ActivePalette.Primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ActivePalette.Primary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = textMuted
                )
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

