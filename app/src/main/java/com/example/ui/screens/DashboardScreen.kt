package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisLog
import com.example.data.model.LogType
import com.example.ui.JarvisViewModel
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
    companionMood: String,
    companionChat: List<JarvisViewModel.CompanionChatMessage>,
    isGeneratingCompanion: Boolean,
    onSendMessage: (String) -> Unit,
    onTriggerCheckIn: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onToggleCallHandler: (Boolean) -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onToggleVoiceWake: (Boolean) -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    LaunchedEffect(companionChat.size) {
        if (companionChat.isNotEmpty()) {
            chatListState.animateScrollToItem(companionChat.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Companion Orb & Emotional Header ---
        item {
            CompanionOrbHeader(
                isServiceActive = isServiceActive,
                isListening = isListening,
                micLevel = micLevel,
                companionMood = companionMood,
                onToggleService = onToggleService,
                onTriggerCheckIn = onTriggerCheckIn,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // --- Live Companion Voice & Chat Console ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ImmersiveCard.copy(alpha = 0.85f))
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp)
                    .testTag("companion_chat_section")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceActive) EmeraldGreen else AlertRed)
                            )
                            Text(
                                text = "COMPANION DIALOGUE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextDim,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PurpleAccent.copy(alpha = 0.15f))
                                .clickable { onTriggerCheckIn() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Check in",
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Ask Check-in",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent
                                )
                            }
                        }
                    }

                    // Conversation Bubble List (Limited to 220dp height for smooth scroll)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ImmersiveItemBg)
                            .padding(10.dp)
                    ) {
                        LazyColumn(
                            state = chatListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(companionChat, key = { it.id }) { msg ->
                                CompanionChatBubble(msg = msg)
                            }

                            if (isGeneratingCompanion) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = CyanAccent,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Replying with love...",
                                            fontSize = 11.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = SlateTextDim
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Suggested Prompts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickChips = listOf(
                            "Who messaged me? 📩",
                            "K gardai xau? 💬",
                            "Khana khayeu? 🍚",
                            "How was your day? 💕",
                            "Remind me to eat lunch 🍽️",
                            "Tell Alex on WhatsApp: on my way",
                            "Go to sleep 🌙"
                        )
                        quickChips.forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(14.dp))
                                    .clickable { onSendMessage(chip) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = chip,
                                    fontSize = 11.sp,
                                    color = SlateTextBright
                                )
                            }
                        }
                    }

                    // Input Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Talk to your companion...", color = SlateTextDim, fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("companion_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = ImmersiveCardBorder,
                                focusedTextColor = SlateTextBright,
                                unfocusedTextColor = SlateTextBright,
                                focusedContainerColor = Color(0xFF0D1527),
                                unfocusedContainerColor = Color(0xFF0D1527)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val text = inputText
                                    inputText = ""
                                    onSendMessage(text)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyanAccent)
                                .testTag("companion_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color(0xFF0A0F1D),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 2-Column Controls Grid ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusCard(
                        title = "Call Handler",
                        statusText = if (isCallHandlerActive) "Human Auto-SMS" else "Disabled",
                        icon = Icons.Default.Call,
                        iconColor = CyanAccent,
                        iconBgColor = CyanAccent.copy(alpha = 0.15f),
                        isActive = isCallHandlerActive,
                        onToggle = onToggleCallHandler,
                        modifier = Modifier.weight(1f),
                        testTag = "call_handler_card"
                    )

                    StatusCard(
                        title = "App Listener",
                        statusText = if (isNotificationActive) "WhatsApp / Insta" else "Disabled",
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
                    StatusCard(
                        title = "Voice Wake",
                        statusText = if (isVoiceWakeActive) "\"Hey Jarvis / Babe\"" else "Muted",
                        icon = Icons.Default.Mic,
                        iconColor = EmeraldGreen,
                        iconBgColor = EmeraldGreen.copy(alpha = 0.15f),
                        isActive = isVoiceWakeActive,
                        onToggle = onToggleVoiceWake,
                        modifier = Modifier.weight(1f),
                        testTag = "voice_wake_card"
                    )

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

        // --- Recent Activity Section ---
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
fun CompanionChatBubble(msg: JarvisViewModel.CompanionChatMessage) {
    val isUser = msg.sender == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .background(
                    if (isUser) Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)))
                    else Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF334155)))
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = msg.message,
                fontSize = 12.sp,
                color = Color.White
            )
        }

        // Display structured action badge if an outgoing message was dispatched
        if (msg.outgoingAction != null) {
            val action = msg.outgoingAction
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldGreen.copy(alpha = 0.15f))
                    .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Dispatched to ${action.recipient ?: "Contact"} on ${action.platform ?: "App"}: \"${action.messageText}\"",
                    fontSize = 10.sp,
                    color = EmeraldGreen,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun CompanionOrbHeader(
    isServiceActive: Boolean,
    isListening: Boolean,
    micLevel: Float,
    companionMood: String,
    onToggleService: (Boolean) -> Unit,
    onTriggerCheckIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF131E3A),
                        Color(0xFF0A0F1D)
                    )
                )
            )
            .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Glowing Animated Companion Orb
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(if (isListening) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                CyanAccent.copy(alpha = 0.8f),
                                PurpleAccent.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                    .clickable { onToggleService(!isServiceActive) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(2.dp, CyanAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Companion Heart",
                        tint = if (isServiceActive) Color(0xFFFF4081) else SlateTextDim,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Right: Identity & Mood
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "JARVIS COMPANION",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = SlateTextBright,
                    letterSpacing = 1.sp
                )

                Text(
                    text = companionMood,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanAccent
                )

                Text(
                    text = if (isListening) "Listening for 'Hey Jarvis' / 'Go to sleep'" else "Standby (Tap heart to wake)",
                    fontSize = 10.sp,
                    color = SlateTextDim
                )
            }
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
