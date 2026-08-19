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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var callerName by remember { mutableStateOf("Sarah Connor") }
    var callerNumber by remember { mutableStateOf("+1 (555) 019-2834") }

    val appOptions = listOf(
        "Instagram" to "com.instagram.android",
        "WhatsApp" to "com.whatsapp",
        "Messenger" to "com.facebook.orca",
        "Telegram" to "org.telegram.messenger",
        "SMS Messages" to "com.google.android.apps.messaging"
    )
    var selectedAppIndex by remember { mutableStateOf(0) }
    var isAppDropdownExpanded by remember { mutableStateOf(false) }
    var senderName by remember { mutableStateOf("Alex Mercer") }
    var incomingMessage by remember { mutableStateOf("Hey! Are you free for the project sync call right now?") }

    val smsTemplate by viewModel.smsTemplate.collectAsState(initial = "")
    val notifTemplate by viewModel.notificationTemplate.collectAsState(initial = "")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
            .padding(horizontal = 16.dp)
            .testTag("simulation_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ImmersiveCard)
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Interactive AI Test Studio",
                            fontWeight = FontWeight.Bold,
                            color = SlateTextBright,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Powered by Gemini 2.5 Flash contextual auto-responses",
                            color = CyanAccent,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Test 1: Missed Call & Gemini Auto-SMS
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ImmersiveCard.copy(alpha = 0.7f))
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp)
                    .testTag("test_missed_call_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AlertRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneMissed,
                                    contentDescription = null,
                                    tint = AlertRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "1. Missed Call & Gemini AI Auto-SMS",
                                fontWeight = FontWeight.Bold,
                                color = SlateTextBright,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Triggers alert tone + TTS voice announcement + Gemini AI generates custom excuse SMS.",
                        fontSize = 12.sp,
                        color = SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = callerName,
                            onValueChange = { callerName = it },
                            label = { Text("Caller Name", color = SlateTextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_caller_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ImmersiveItemBg,
                                unfocusedContainerColor = ImmersiveItemBg,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = ImmersiveCardBorder,
                                focusedTextColor = SlateTextBright,
                                unfocusedTextColor = SlateTextBright
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = callerNumber,
                            onValueChange = { callerNumber = it },
                            label = { Text("Phone Number", color = SlateTextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_caller_number_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ImmersiveItemBg,
                                unfocusedContainerColor = ImmersiveItemBg,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = ImmersiveCardBorder,
                                focusedTextColor = SlateTextBright,
                                unfocusedTextColor = SlateTextBright
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ImmersiveItemBg)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Fallback Template: \"$smsTemplate\"",
                            fontSize = 11.sp,
                            color = SlateTextDim
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.simulateMissedCall(callerName, callerNumber)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_missed_call_sim_button")
                    ) {
                        Icon(imageVector = Icons.Default.PhoneMissed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Missed Call & AI Excuse", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Test 2: Instagram / Messaging Auto-Reply with Gemini
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ImmersiveCard.copy(alpha = 0.7f))
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp)
                    .testTag("test_notif_reply_card")
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurpleAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "2. Instagram / DM Auto-Reply (Gemini AI)",
                            fontWeight = FontWeight.Bold,
                            color = SlateTextBright,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Intercepts DM notifications and uses Gemini 2.5 Flash to synthesize a smart inline reply.",
                        fontSize = 12.sp,
                        color = SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = isAppDropdownExpanded,
                        onExpandedChange = { isAppDropdownExpanded = !isAppDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = appOptions[selectedAppIndex].first,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Application", color = SlateTextMuted) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAppDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("sim_app_dropdown"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ImmersiveItemBg,
                                unfocusedContainerColor = ImmersiveItemBg,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = ImmersiveCardBorder,
                                focusedTextColor = SlateTextBright,
                                unfocusedTextColor = SlateTextBright
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isAppDropdownExpanded,
                            onDismissRequest = { isAppDropdownExpanded = false }
                        ) {
                            appOptions.forEachIndexed { index, pair ->
                                DropdownMenuItem(
                                    text = { Text("${pair.first} (${pair.second})") },
                                    onClick = {
                                        selectedAppIndex = index
                                        isAppDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = senderName,
                        onValueChange = { senderName = it },
                        label = { Text("Sender Username / Handle", color = SlateTextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sim_sender_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveItemBg,
                            unfocusedContainerColor = ImmersiveItemBg,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ImmersiveCardBorder,
                            focusedTextColor = SlateTextBright,
                            unfocusedTextColor = SlateTextBright
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick Presets (Roman Nepali & English):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextMuted
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "k gardai xau? call gara na" to "Aayush",
                            "khana khayeu? vetne ho?" to "Pooja",
                            "Hey! Sync call now?" to "Alex"
                        ).forEach { (msg, sender) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("preset_${sender}")
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 10.sp,
                                    color = CyanAccent,
                                    modifier = Modifier.clickable {
                                        incomingMessage = msg
                                        senderName = sender
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = incomingMessage,
                        onValueChange = { incomingMessage = it },
                        label = { Text("Incoming Direct Message", color = SlateTextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sim_message_text_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveItemBg,
                            unfocusedContainerColor = ImmersiveItemBg,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ImmersiveCardBorder,
                            focusedTextColor = SlateTextBright,
                            unfocusedTextColor = SlateTextBright
                        ),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ImmersiveItemBg)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Fallback Template: \"$notifTemplate\"",
                            fontSize = 11.sp,
                            color = SlateTextDim
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.simulateNotificationAutoReply(
                                appOptions[selectedAppIndex].second,
                                senderName,
                                incomingMessage
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_notif_reply_sim_button")
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = ImmersiveBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Intercept & Gemini Reply", color = ImmersiveBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Test 3: Voice Wake-Word Activation
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ImmersiveCard.copy(alpha = 0.7f))
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp)
                    .testTag("test_wake_word_card")
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "3. Voice Wake-Word (\"Hey Jarvis\")",
                            fontWeight = FontWeight.Bold,
                            color = SlateTextBright,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Plays arc-reactor confirmation audio tone + audible TTS greeting response.",
                        fontSize = 12.sp,
                        color = SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateWakeWordTrigger() },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("run_wake_word_sim_button")
                        ) {
                            Text("Test Wake Trigger", color = ImmersiveBg, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.testTts("Hello! I am Jarvis, your hands-free background assistant.") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_tts_button")
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = ImmersiveBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test TTS Voice", color = ImmersiveBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
