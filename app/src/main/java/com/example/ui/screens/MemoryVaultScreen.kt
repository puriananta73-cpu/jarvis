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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemoryCategory
import com.example.data.model.UserMemory
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val MemoryCategory.displayName: String
    get() = when (this) {
        MemoryCategory.STYLE_SLANG -> "Slang & Tone"
        MemoryCategory.PERSONAL_FACT -> "Personal Facts"
        MemoryCategory.RELATIONSHIP -> "Relationships"
        MemoryCategory.DAILY_ROUTINE -> "Daily Routine"
        MemoryCategory.SEARCH_QUERY -> "Search Knowledge"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryVaultScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.userMemories.collectAsState()
    val learnedTone by viewModel.learnedToneSamples.collectAsState(initial = "")
    var selectedFilter by remember { mutableStateOf<MemoryCategory?>(null) }
    var newFactText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MemoryCategory.PERSONAL_FACT) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val filteredMemories = if (selectedFilter == null) {
        memories
    } else {
        memories.filter { it.category == selectedFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
            .padding(horizontal = 16.dp)
            .testTag("memory_vault_screen"),
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Memory & Personality Vault",
                            fontWeight = FontWeight.Bold,
                            color = SlateTextBright,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${memories.size} facts & rules actively shaping AI auto-replies",
                            color = CyanAccent,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Add New Fact / Rule Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ImmersiveCard)
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Teach AI a New Fact or Rule",
                        fontWeight = FontWeight.Bold,
                        color = SlateTextBright,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = newFactText,
                        onValueChange = { newFactText = it },
                        label = { Text("E.g. 'I love black coffee' or 'Use 'babal' for awesome'") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("memory_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ImmersiveCardBorder,
                            focusedTextColor = SlateTextBright,
                            unfocusedTextColor = SlateTextBright,
                            focusedContainerColor = ImmersiveItemBg,
                            unfocusedContainerColor = ImmersiveItemBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = isCategoryDropdownExpanded,
                            onExpandedChange = { isCategoryDropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedCategory.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = ImmersiveCardBorder,
                                    focusedTextColor = SlateTextBright,
                                    unfocusedTextColor = SlateTextBright,
                                    focusedContainerColor = ImmersiveItemBg,
                                    unfocusedContainerColor = ImmersiveItemBg
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = isCategoryDropdownExpanded,
                                onDismissRequest = { isCategoryDropdownExpanded = false },
                                modifier = Modifier.background(ImmersiveCard)
                            ) {
                                MemoryCategory.values().forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.displayName, color = SlateTextBright) },
                                        onClick = {
                                            selectedCategory = cat
                                            isCategoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (newFactText.isNotBlank()) {
                                    viewModel.addManualMemory(selectedCategory, newFactText.trim())
                                    newFactText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("save_memory_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ImmersiveBg)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Teach", color = ImmersiveBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CategoryChip(
                    label = "All (${memories.size})",
                    isSelected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                MemoryCategory.values().take(3).forEach { cat ->
                    val count = memories.count { it.category == cat }
                    CategoryChip(
                        label = "${cat.displayName} ($count)",
                        isSelected = selectedFilter == cat,
                        onClick = { selectedFilter = cat }
                    )
                }
            }
        }

        // Learned Tone Samples Preview
        if (learnedTone.isNotBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ImmersiveCard.copy(alpha = 0.6f))
                        .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Learned Tone DNA (Background Chat Stream)", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(
                            text = learnedTone,
                            color = SlateTextDim,
                            fontSize = 12.sp,
                            maxLines = 3
                        )
                    }
                }
            }
        }

        if (filteredMemories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SlateTextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No memories in this category yet",
                            color = SlateTextMuted,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Talk to Gemini Live or receive chats to train memories automatically!",
                            color = SlateTextDim,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredMemories, key = { it.id }) { memory ->
                MemoryItemCard(
                    memory = memory,
                    onDelete = { viewModel.deleteUserMemory(memory.id) }
                )
            }

            item {
                Button(
                    onClick = { viewModel.clearAllMemories() },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("clear_all_memories_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All AI Memories", color = AlertRed, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CyanAccent else ImmersiveCard)
            .border(1.dp, if (isSelected) CyanAccent else ImmersiveCardBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) ImmersiveBg else SlateTextDim
        )
    }
}

@Composable
fun MemoryItemCard(
    memory: UserMemory,
    onDelete: () -> Unit
) {
    val (catColor, catIcon) = when (memory.category) {
        MemoryCategory.STYLE_SLANG -> AmberAccent to Icons.Default.Chat
        MemoryCategory.PERSONAL_FACT -> CyanAccent to Icons.Default.Person
        MemoryCategory.RELATIONSHIP -> PurpleAccent to Icons.Default.AutoAwesome
        MemoryCategory.DAILY_ROUTINE -> EmeraldGreen to Icons.Default.Schedule
        MemoryCategory.SEARCH_QUERY -> CyanAccent to Icons.Default.Search
    }

    val timeFormatted = remember(memory.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(memory.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ImmersiveCard)
            .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("memory_card_${memory.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = memory.category.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = catColor
                    )
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = SlateTextDim
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = memory.factOrRule,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateTextBright
                )

                if (memory.sourceContext.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Source: ${memory.sourceContext}",
                        fontSize = 10.sp,
                        color = SlateTextDim
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("delete_memory_${memory.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Memory",
                    tint = SlateTextDim,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
