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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun ActivityLogScreen(
    logs: List<JarvisLog>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<LogType?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filterOptions = listOf(
        "All" to null,
        "Missed Calls" to LogType.CALL_MISSED,
        "Auto-SMS" to LogType.SMS_AUTO_SENT,
        "Auto-Reply" to LogType.AUTO_REPLY_SENT,
        "Notifications" to LogType.NOTIFICATION_RECEIVED,
        "Wake Word" to LogType.WAKE_WORD_DETECTED
    )

    val filteredLogs = logs.filter { log ->
        val matchesType = selectedFilter == null || log.type == selectedFilter
        val matchesSearch = if (searchQuery.isBlank()) true else {
            log.title.contains(searchQuery, ignoreCase = true) ||
            log.description.contains(searchQuery, ignoreCase = true) ||
            log.sourcePackage.contains(searchQuery, ignoreCase = true)
        }
        matchesType && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
            .padding(horizontal = 16.dp)
            .testTag("activity_log_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Activity Log",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Text(
                    text = "${filteredLogs.size} recorded events",
                    fontSize = 11.sp,
                    color = SlateTextMuted
                )
            }

            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = onClearLogs,
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear All",
                        tint = AlertRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear",
                        fontSize = 12.sp,
                        color = AlertRed.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search logs or messages...", color = SlateTextDim, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SlateTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = SlateTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ImmersiveCard,
                unfocusedContainerColor = ImmersiveCard.copy(alpha = 0.6f),
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = ImmersiveCardBorder,
                focusedTextColor = SlateTextBright,
                unfocusedTextColor = SlateTextBright
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("activity_search_field")
        )

        // Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { (label, type) ->
                val isSelected = selectedFilter == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CyanAccent else ImmersiveCard)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CyanAccent else ImmersiveCardBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedFilter = type }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ImmersiveBg else SlateTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Logs List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ImmersiveCard)
                            .border(1.dp, ImmersiveCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = SlateTextDim,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No activities logged yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextBright
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Simulate incoming calls or notifications in Test Studio",
                        fontSize = 11.sp,
                        color = SlateTextDim
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("activity_logs_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    JarvisLogItemCard(log = log)
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun JarvisLogItemCard(
    log: JarvisLog,
    modifier: Modifier = Modifier
) {
    val (appIcon, iconColor, bgIconColor) = when (log.type) {
        LogType.CALL_MISSED -> Triple(Icons.Default.PhoneMissed, AlertRed, AlertRed.copy(alpha = 0.15f))
        LogType.CALL_INCOMING -> Triple(Icons.Default.Call, CyanAccent, CyanAccent.copy(alpha = 0.15f))
        LogType.SMS_AUTO_SENT -> Triple(Icons.Default.Send, CyanAccent, CyanAccent.copy(alpha = 0.15f))
        LogType.AUTO_REPLY_SENT -> Triple(Icons.Default.Chat, EmeraldGreen, EmeraldGreen.copy(alpha = 0.15f))
        LogType.NOTIFICATION_RECEIVED -> Triple(Icons.Default.Notifications, PurpleAccent, PurpleAccent.copy(alpha = 0.15f))
        LogType.WAKE_WORD_DETECTED -> Triple(Icons.Default.Mic, AmberAccent, AmberAccent.copy(alpha = 0.15f))
        LogType.SERVICE_EVENT -> Triple(Icons.Default.Notifications, CyanAccent, CyanAccent.copy(alpha = 0.15f))
    }

    val timeFormatted = remember(log.timestamp) {
        val diff = System.currentTimeMillis() - log.timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(log.timestamp))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ImmersiveCard.copy(alpha = 0.6f))
            .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(18.dp))
            .padding(12.dp)
            .testTag("log_item_${log.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Event Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgIconColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = appIcon,
                    contentDescription = log.type.name,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextBright,
                        maxLines = 1
                    )
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = SlateTextDim
                    )
                }

                if (log.description.isNotEmpty()) {
                    Text(
                        text = log.description,
                        fontSize = 11.sp,
                        color = SlateTextMuted,
                        maxLines = 3
                    )
                }

                if (log.type == LogType.AUTO_REPLY_SENT || log.type == LogType.SMS_AUTO_SENT) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ImmersiveItemBg)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Auto-Reply Dispatched",
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            color = EmeraldGreen.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
