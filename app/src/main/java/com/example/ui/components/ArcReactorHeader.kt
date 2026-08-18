package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveCard
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.SlateTextBright
import com.example.ui.theme.SlateTextDim
import com.example.ui.theme.SlateTextMuted

@Composable
fun ArcReactorHeader(
    isServiceActive: Boolean,
    isListening: Boolean,
    micLevel: Float,
    onToggleService: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val coreGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_glow"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("arc_reactor_header"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Immersive Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Jarvis Assistant",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanAccent,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isServiceActive) EmeraldGreen else AlertRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isServiceActive) "SYSTEM ACTIVE" else "SYSTEM OFFLINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceActive) SlateTextMuted else AlertRed,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Interactive Profile / Master Power Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isServiceActive) ImmersiveCard else Color(0xFF1E1A22))
                    .border(
                        width = 1.dp,
                        color = if (isServiceActive) ImmersiveCardBorder else AlertRed.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onToggleService(!isServiceActive) }
                    .testTag("power_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isServiceActive) {
                    Text(
                        text = "JS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Turn Jarvis On",
                        tint = AlertRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Centerpiece Orb / Arc Reactor
        Box(
            modifier = Modifier
                .size(160.dp)
                .clickable { onToggleService(!isServiceActive) },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val centerOffset = Offset(size.width / 2, size.height / 2)
                val baseRadius = 60.dp.toPx()

                if (isServiceActive) {
                    // Soft outer glow cloud
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CyanAccent.copy(alpha = 0.22f * coreGlowAlpha),
                                CyanGlow.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = baseRadius * 1.35f * pulseScale
                        ),
                        radius = baseRadius * 1.35f * pulseScale,
                        center = centerOffset
                    )

                    // Outer segmented ring
                    for (i in 0 until 6) {
                        val startAngle = rotationAngle + (i * 60f)
                        drawArc(
                            color = CyanAccent.copy(alpha = 0.45f),
                            startAngle = startAngle,
                            sweepAngle = 36f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                            size = size
                        )
                    }

                    // Middle orb container
                    drawCircle(
                        color = Color(0x330F172A),
                        radius = baseRadius * 0.85f,
                        center = centerOffset
                    )
                    drawCircle(
                        color = CyanAccent.copy(alpha = 0.35f),
                        radius = baseRadius * 0.85f,
                        style = Stroke(width = 1.5.dp.toPx()),
                        center = centerOffset
                    )

                    // Dynamic sound reactive ring
                    val soundRadius = baseRadius * 0.65f + (micLevel * 1.6f).coerceAtMost(14f)
                    drawCircle(
                        color = CyanAccent.copy(alpha = 0.8f),
                        radius = soundRadius,
                        style = Stroke(width = 3.dp.toPx()),
                        center = centerOffset
                    )

                    // Core pulsing light beam
                    drawLine(
                        color = CyanAccent.copy(alpha = coreGlowAlpha),
                        start = Offset(centerOffset.x - 16.dp.toPx(), centerOffset.y),
                        end = Offset(centerOffset.x + 16.dp.toPx(), centerOffset.y),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    // Inactive muted ring
                    drawCircle(
                        color = Color(0xFF1E293B).copy(alpha = 0.5f),
                        radius = baseRadius * 0.8f,
                        style = Stroke(width = 2.dp.toPx()),
                        center = centerOffset
                    )
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = baseRadius * 0.4f,
                        center = centerOffset
                    )
                }
            }

            // Pill Badge at the bottom of the orb
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(if (isServiceActive && isListening) CyanAccent else if (isServiceActive) EmeraldGreen else Color(0xFF334155))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isServiceActive && isListening) "LISTENING" else if (isServiceActive) "STANDBY" else "OFFLINE",
                    color = ImmersiveBg,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Trigger hint
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Say ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SlateTextMuted
            )
            Text(
                text = "\"Hey Jarvis\"",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = CyanAccent
            )
            Text(
                text = " to wake",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SlateTextMuted
            )
        }
    }
}
