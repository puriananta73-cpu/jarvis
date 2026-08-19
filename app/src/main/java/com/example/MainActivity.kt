package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.screens.ActivityLogScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimulationScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SlateTextDim

enum class Screen(val label: String, val icon: ImageVector) {
    DASHBOARD("Overview", Icons.Default.Dashboard),
    SIMULATION("Test Studio", Icons.Default.PlayCircleOutline),
    LOGS("Activity", Icons.Default.ListAlt),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val isServiceRunning by viewModel.isServiceRunning.collectAsState()
                val isListening by viewModel.isWakeWordListening.collectAsState()
                val micLevel by viewModel.micRmsLevel.collectAsState()
                val isCallHandlerActive by viewModel.isMissedCallSmsEnabled.collectAsState(initial = true)
                val isNotificationActive by viewModel.isNotificationReplyEnabled.collectAsState(initial = true)
                val isVoiceWakeActive by viewModel.isVoiceWakeEnabled.collectAsState(initial = true)
                val activityLogs by viewModel.logs.collectAsState()

                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                val companionMood by viewModel.companionMood.collectAsState()
                val companionChat by viewModel.companionChat.collectAsState()
                val isGeneratingCompanion by viewModel.isGeneratingCompanion.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.refreshPermissions()
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ImmersiveBg),
                    containerColor = ImmersiveBg,
                    bottomBar = {
                        ImmersiveBottomNavBar(
                            currentScreen = currentScreen,
                            onScreenSelected = { currentScreen = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                Screen.DASHBOARD -> DashboardScreen(
                                    isServiceActive = isServiceRunning,
                                    isListening = isListening,
                                    micLevel = micLevel,
                                    isCallHandlerActive = isCallHandlerActive,
                                    isNotificationActive = isNotificationActive,
                                    isVoiceWakeActive = isVoiceWakeActive,
                                    recentLogs = activityLogs,
                                    companionMood = companionMood,
                                    companionChat = companionChat,
                                    isGeneratingCompanion = isGeneratingCompanion,
                                    onSendMessage = { viewModel.sendCompanionMessage(it) },
                                    onTriggerCheckIn = { viewModel.triggerProactiveCheckIn() },
                                    onToggleService = { viewModel.toggleMasterService(it) },
                                    onToggleCallHandler = { viewModel.toggleMissedCallSms(it) },
                                    onToggleNotification = { viewModel.toggleNotificationReply(it) },
                                    onToggleVoiceWake = { viewModel.toggleVoiceWake(it) },
                                    onNavigateToLogs = { currentScreen = Screen.LOGS },
                                    onNavigateToSimulation = { currentScreen = Screen.SIMULATION }
                                )
                                Screen.SIMULATION -> SimulationScreen(
                                    viewModel = viewModel
                                )
                                Screen.LOGS -> ActivityLogScreen(
                                    logs = activityLogs,
                                    onClearLogs = { viewModel.clearAllLogs() }
                                )
                                Screen.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    onRequestNotificationAccess = { openNotificationListenerSettings() },
                                    onRequestMicrophonePermission = { requestAppPermissions() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    private fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@Composable
fun ImmersiveBottomNavBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .border(
                width = 1.dp,
                color = ImmersiveCardBorder.copy(alpha = 0.5f)
            )
            .background(ImmersiveBg)
            .padding(vertical = 10.dp)
            .testTag("immersive_bottom_nav")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.values().forEach { screen ->
                val isSelected = currentScreen == screen
                Column(
                    modifier = Modifier
                        .clickable { onScreenSelected(screen) }
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("nav_item_${screen.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(CyanAccent)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        tint = if (isSelected) CyanAccent else SlateTextDim,
                        modifier = Modifier.size(22.dp)
                    )

                    Text(
                        text = screen.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) CyanAccent else SlateTextDim
                    )
                }
            }
        }
    }
}
