package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    onRequestNotificationAccess: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val smsTemplate by viewModel.smsTemplate.collectAsState(initial = "")
    val notifTemplate by viewModel.notificationTemplate.collectAsState(initial = "")
    val isAnnounceMessages by viewModel.isAnnounceMessagesEnabled.collectAsState(initial = true)
    val learnedToneSamples by viewModel.learnedToneSamples.collectAsState(initial = "")
    val permissions by viewModel.permissions.collectAsState()
    val ttsSpeedPref by viewModel.ttsSpeed.collectAsState(initial = 1.0f)
    val ttsPitchPref by viewModel.ttsPitch.collectAsState(initial = 1.0f)

    var editableSmsTemplate by remember(smsTemplate) { mutableStateOf(smsTemplate) }
    var editableNotifTemplate by remember(notifTemplate) { mutableStateOf(notifTemplate) }
    var editableToneSamples by remember(learnedToneSamples) { mutableStateOf(learnedToneSamples) }
    var currentTtsSpeed by remember(ttsSpeedPref) { mutableFloatStateOf(ttsSpeedPref) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }
        }

        // Permissions Status Overview
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ImmersiveCard.copy(alpha = 0.7f))
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp)
                    .testTag("permissions_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Permission Handlers",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextBright
                        )
                    }

                    // Notification Access row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ImmersiveItemBg)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Listener Access",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateTextBright
                            )
                            Text(
                                text = if (permissions.hasNotificationListener) "Granted & Active" else "Requires Android Permission",
                                fontSize = 10.sp,
                                color = if (permissions.hasNotificationListener) EmeraldGreen else AlertRed
                            )
                        }
                        if (!permissions.hasNotificationListener) {
                            Button(
                                onClick = onRequestNotificationAccess,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                modifier = Modifier.testTag("request_notif_permission_button")
                            ) {
                                Text("Grant", fontSize = 11.sp, color = ImmersiveBg, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Microphone Access row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ImmersiveItemBg)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Microphone Recording",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateTextBright
                            )
                            Text(
                                text = if (permissions.hasAudio) "Granted & Active" else "Required for \"Hey Jarvis\" hotword",
                                fontSize = 10.sp,
                                color = if (permissions.hasAudio) EmeraldGreen else AlertRed
                            )
                        }
                        if (!permissions.hasAudio) {
                            Button(
                                onClick = onRequestMicrophonePermission,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                modifier = Modifier.testTag("request_mic_permission_button")
                            ) {
                                Text("Grant", fontSize = 11.sp, color = ImmersiveBg, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Automated Response Templates
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ImmersiveCard.copy(alpha = 0.7f))
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp)
                    .testTag("response_templates_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Auto-Reply Templates",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextBright
                        )
                    }

                    // SMS Template
                    Text(
                        text = "Missed Call Auto-SMS Excuse",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextMuted
                    )
                    OutlinedTextField(
                        value = editableSmsTemplate,
                        onValueChange = {
                            editableSmsTemplate = it
                            viewModel.updateSmsTemplate(it)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveItemBg,
                            unfocusedContainerColor = ImmersiveItemBg,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ImmersiveCardBorder,
                            focusedTextColor = SlateTextBright,
                            unfocusedTextColor = SlateTextBright
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_template_input"),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Notification Template
                    Text(
                        text = "Notification Auto-Reply Message",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextMuted
                    )
                    OutlinedTextField(
                        value = editableNotifTemplate,
                        onValueChange = {
                            editableNotifTemplate = it
                            viewModel.updateNotificationTemplate(it)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveItemBg,
                            unfocusedContainerColor = ImmersiveItemBg,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ImmersiveCardBorder,
                            focusedTextColor = SlateTextBright,
                            unfocusedTextColor = SlateTextBright
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notif_template_input"),
                        minLines = 2
                    )
                }
            }
        }

        // TTS Voice Settings
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ImmersiveCard.copy(alpha = 0.7f))
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp)
                    .testTag("tts_settings_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    .background(PurpleAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Announce Messages Audibly",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextBright
                                )
                                Text(
                                    text = "Tells you who messaged in real-time",
                                    fontSize = 11.sp,
                                    color = SlateTextDim
                                )
                            }
                        }
                        Switch(
                            checked = isAnnounceMessages,
                            onCheckedChange = { viewModel.toggleAnnounceMessages(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PurpleAccent,
                                checkedTrackColor = PurpleAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = SlateTextDim,
                                uncheckedTrackColor = ImmersiveItemBg
                            ),
                            modifier = Modifier.testTag("announce_messages_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "TTS Voice Speed: ${String.format("%.1fx", currentTtsSpeed)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextBright
                    )
                    Slider(
                        value = currentTtsSpeed,
                        onValueChange = {
                            currentTtsSpeed = it
                            viewModel.updateTtsSettings(ttsPitchPref, it)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAccent,
                            activeTrackColor = CyanAccent,
                            inactiveTrackColor = ImmersiveItemBg
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("tts_speed_slider")
                    )
                    Button(
                        onClick = {
                            viewModel.testTts("Babe, Jarvis voice is calibrated at ${String.format("%.1fx", currentTtsSpeed)} speed.")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Preview Calibrated Voice", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Background Tone Training & Roman Nepali DNA
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ImmersiveCard.copy(alpha = 0.7f))
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp)
                    .testTag("tone_training_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Learned Tone & Roman Nepali DNA",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextBright
                            )
                            Text(
                                text = "Trains in background to sound exactly like you",
                                fontSize = 11.sp,
                                color = SlateTextDim
                            )
                        }
                    }

                    Text(
                        text = "Training Samples (Slang, Roman Nepali, Text Style):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextMuted
                    )

                    OutlinedTextField(
                        value = editableToneSamples,
                        onValueChange = {
                            editableToneSamples = it
                            viewModel.updateLearnedToneSamples(it)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveItemBg,
                            unfocusedContainerColor = ImmersiveItemBg,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ImmersiveCardBorder,
                            focusedTextColor = SlateTextBright,
                            unfocusedTextColor = SlateTextBright
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tone_samples_input"),
                        minLines = 3,
                        placeholder = {
                            Text("e.g. k gardai xau? khana khayeu? la hai babe...", color = SlateTextDim, fontSize = 12.sp)
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
