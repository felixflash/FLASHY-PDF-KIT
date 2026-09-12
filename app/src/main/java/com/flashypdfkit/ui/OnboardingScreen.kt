package com.flashypdfkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.ui.components.GradientButton
import com.flashypdfkit.ui.theme.AccentCyan
import com.flashypdfkit.ui.theme.AccentPurple
import com.flashypdfkit.ui.theme.DarkTextMuted

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }

    val icon = when (step) {
        1 -> "⚡"
        2 -> "🔒"
        else -> "🚀"
    }

    val title = when (step) {
        1 -> "All-in-One PDF Kit"
        2 -> "100% Private & Offline"
        else -> "Ready to Supercharge"
    }

    val subtitle = when (step) {
        1 -> "Merge, split, compress, sign, and convert PDFs with lightning speed right on your device."
        2 -> "Your documents never leave your phone. Zero cloud uploads, zero privacy tracking."
        else -> "Unlock your full PDF productivity workflow now with FlashyPDF Kit."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AccentPurple, AccentCyan))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 56.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = DarkTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                for (i in 1..3) {
                    Box(
                        modifier = Modifier
                            .size(if (i == step) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i == step) AccentPurple else Color.Gray.copy(alpha = 0.3f))
                    )
                }
            }

            GradientButton(
                text = if (step < 3) "Continue" else "Get Started",
                onClick = {
                    if (step < 3) step++ else onFinish()
                }
            )
        }
    }
}
