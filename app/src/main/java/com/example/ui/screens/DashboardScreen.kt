package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisLog
import com.example.data.model.LogType
import com.example.ui.components.ArcReactorHeader
import com.example.ui.components.StatusCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveCard
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.ImmersiveItemBg
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SlateTextBright
import com.example.ui.theme.SlateTextDim
import com.example.ui.theme.SlateTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    isServiceActive: Boolean,
    isListening: Boolean,
    micLevel: Float,
    isCallHandlerActive: Boolean,
    isNotificationActive: Boolean,
    isVoiceWakeActive: Boolean,
    recentLogs: List<JarvisLog>,
    onToggleService: (Boolean) -> Unit,
    onToggleCallHandler: (Boolean) -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onToggleVoiceWake: (Boolean) -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Arc Reactor Header & Voice Orb
        item {
            ArcReactorHeader(
                isServiceActive = isServiceActive,
                isListening = isListening,
                micLevel = micLevel,
                onToggleService = onToggleService,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 2-Column Status Grid (Immersive UI Style)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Call Handler Card
                    StatusCard(
                        title = "Call Handler",
                        statusText = if (isCallHandlerActive) "Auto-Reply ON" else "Disabled",
                        icon = Icons.Default.Call,
                        iconColor = CyanAccent,
                        iconBgColor = CyanAccent.copy(alpha = 0.15f),
                        isActive = isCallHandlerActive,
                        onToggle = onToggleCallHandler,
                        modifier = Modifier.weight(1f),
                        testTag = "call_handler_card"
                    )

                    // Notification Listener Card
                    StatusCard(
                        title = "App Listener",
                        statusText = if (isNotificationActive) "Monitoring Apps" else "Disabled",
                        icon = Icons.Default.Notifications,
                        iconColor = PurpleAccent,
                        iconBgColor = PurpleAccent.copy(alpha = 0.15f),
                        isActive = isNotificationActive,
                        onToggle = onToggleNotification,
                        modifier = Modifier.weight(1f),
                        testTag = "notification_listener_card"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Voice Wake Card
                    StatusCard(
                        title = "Voice Wake",
                        statusText = if (isVoiceWakeActive) "Keyword Active" else "Muted",
                        icon = Icons.Default.Mic,
                        iconColor = EmeraldGreen,
                        iconBgColor = EmeraldGreen.copy(alpha = 0.15f),
                        isActive = isVoiceWakeActive,
                        onToggle = onToggleVoiceWake,
                        modifier = Modifier.weight(1f),
                        testTag = "voice_wake_card"
                    )

                    // Test Studio Quick Launcher
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(ImmersiveCard.copy(alpha = 0.65f))
                            .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(22.dp))
                            .clickable { onNavigateToSimulation() }
                            .padding(14.dp)
                            .testTag("quick_simulation_button")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CyanAccent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Test Studio",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Open",
                                    tint = SlateTextDim,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "Test Studio",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextBright
                            )

                            Text(
                                text = "Simulate events",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = CyanAccent
                            )
                        }
                    }
                }
            }
        }

        // Recent Activity Section (Matching Immersive UI HTML Layout)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ImmersiveCard.copy(alpha = 0.75f))
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp)
                    .testTag("recent_activity_section")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT ACTIVITY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextDim,
                            letterSpacing = 1.5.sp
                        )

                        Text(
                            text = "View All",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            modifier = Modifier
                                .clickable { onNavigateToLogs() }
                                .padding(4.dp)
                                .testTag("view_all_logs_button")
                        )
                    }

                    if (recentLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent events recorded",
                                fontSize = 12.sp,
                                color = SlateTextDim
                            )
                        }
                    } else {
                        recentLogs.take(3).forEach { log ->
                            RecentLogItemMini(log = log)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun RecentLogItemMini(
    log: JarvisLog,
    modifier: Modifier = Modifier
) {
    val (icon, tintColor) = when (log.type) {
        LogType.CALL_MISSED -> Pair(Icons.Default.PhoneMissed, AlertRed)
        LogType.CALL_INCOMING -> Pair(Icons.Default.Call, CyanAccent)
        LogType.SMS_AUTO_SENT -> Pair(Icons.Default.Send, CyanAccent)
        LogType.AUTO_REPLY_SENT -> Pair(Icons.Default.Chat, EmeraldGreen)
        LogType.NOTIFICATION_RECEIVED -> Pair(Icons.Default.Notifications, PurpleAccent)
        LogType.WAKE_WORD_DETECTED -> Pair(Icons.Default.Mic, AmberAccent)
        LogType.SERVICE_EVENT -> Pair(Icons.Default.Notifications, CyanAccent)
    }

    val timeFormatted = remember(log.timestamp) {
        val diff = System.currentTimeMillis() - log.timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ImmersiveItemBg)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateTextBright,
                    maxLines = 1
                )
                Text(
                    text = log.description,
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    color = SlateTextMuted,
                    maxLines = 1
                )
            }

            Text(
                text = timeFormatted,
                fontSize = 9.sp,
                color = SlateTextDim
            )
        }
    }
}
