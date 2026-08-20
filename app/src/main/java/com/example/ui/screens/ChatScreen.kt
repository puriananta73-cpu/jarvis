package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ChatPersona
import com.example.data.repository.GroundingCitation
import com.example.ui.JarvisViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

private val DarkNavy = Color(0xFF070B14)
private val CardBg = Color(0xFF0F172A)
private val CardBorder = Color(0xFF1E293B)
private val CyanColor = Color(0xFF06B6D4)
private val PurpleColor = Color(0xFFA855F7)
private val AmberColor = Color(0xFFF59E0B)
private val EmeraldColor = Color(0xFF10B981)
private val SlateDim = Color(0xFF94A3B8)
private val SlateBright = Color(0xFFF8FAFC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val chatMessages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isChatbotGenerating.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val isSearchGrounding by viewModel.isSearchGroundingEnabled.collectAsState()
    val isMapsGrounding by viewModel.isMapsGroundingEnabled.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showModelSheet by remember { mutableStateOf(false) }
    var showPersonaSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavy)
            .testTag("chat_screen")
    ) {
        // --- Top Bar with Model & Persona Controls ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .border(1.dp, CardBorder)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(CyanColor)
                        )
                        Text(
                            text = "GEMINI MULTI-TURN CHAT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = SlateBright,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = selectedPersona.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = CyanColor
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBorder)
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Chat",
                            tint = SlateDim,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // --- Persona & Model Selector Chips Row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Persona Selector Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurpleColor.copy(alpha = 0.15f))
                        .border(1.dp, PurpleColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { showPersonaSheet = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("persona_selector_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = PurpleColor, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Role: ${selectedPersona.title.take(16)}...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleColor
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PurpleColor, modifier = Modifier.size(14.dp))
                    }
                }

                // Model Selector Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyanColor.copy(alpha = 0.15f))
                        .border(1.dp, CyanColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { showModelSheet = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("model_selector_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanColor, modifier = Modifier.size(14.dp))
                        Text(
                            text = when (selectedModel) {
                                "gemini-3.1-pro-preview" -> "Gemini 3.1 Pro"
                                "gemini-3.1-flash-lite-preview" -> "Gemini 3.1 Flash-Lite"
                                else -> "Gemini 3.5 Flash"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanColor
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CyanColor, modifier = Modifier.size(14.dp))
                    }
                }

                // Search Grounding Toggle Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSearchGrounding) EmeraldColor.copy(alpha = 0.15f) else CardBorder.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            if (isSearchGrounding) EmeraldColor else SlateDim.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.toggleSearchGrounding(!isSearchGrounding) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("toggle_search_grounding")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isSearchGrounding) EmeraldColor else SlateDim,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isSearchGrounding) "Google Search ON" else "Search OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSearchGrounding) EmeraldColor else SlateDim
                        )
                    }
                }

                // Maps Grounding Toggle Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isMapsGrounding) AmberColor.copy(alpha = 0.15f) else CardBorder.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            if (isMapsGrounding) AmberColor else SlateDim.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.toggleMapsGrounding(!isMapsGrounding) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("toggle_maps_grounding")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = if (isMapsGrounding) AmberColor else SlateDim,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isMapsGrounding) "Google Maps ON" else "Maps OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMapsGrounding) AmberColor else SlateDim
                        )
                    }
                }
            }
        }

        // --- Scrollable Chat Messages Thread ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("chat_messages_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    onOpenUrl = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCopyText = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (isGenerating) {
                item {
                    ThinkingIndicator(
                        modelName = when (selectedModel) {
                            "gemini-3.1-pro-preview" -> "Gemini 3.1 Pro (Deep Reasoning...)"
                            "gemini-3.1-flash-lite-preview" -> "Gemini 3.1 Flash-Lite (Fast...)"
                            else -> "Gemini 3.5 Flash (Synthesizing...)"
                        }
                    )
                }
            }
        }

        // --- Quick Suggested Prompts Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "Search latest Nepal tech news 🌐",
                "Find best cafes in Thamel Kathmandu 📍",
                "Explain Quantum Computing simply 🧠",
                "K gardai xau sathi? Khana khayeu? 🇳🇵",
                "Solve Kotlin Coroutines Race Condition 💻",
                "What's the weather today? ☀️"
            )

            suggestions.forEach { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBorder)
                        .clickable { viewModel.sendChatMessage(prompt) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = SlateBright
                    )
                }
            }
        }

        // --- Bottom Input Area ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .border(1.dp, CardBorder)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Ask anything, search web, or ground maps...",
                        color = SlateDim,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanColor,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = SlateBright,
                    unfocusedTextColor = SlateBright,
                    focusedContainerColor = DarkNavy,
                    unfocusedContainerColor = DarkNavy
                ),
                shape = RoundedCornerShape(16.dp),
                maxLines = 4
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        viewModel.sendChatMessage(text)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(CyanColor, PurpleColor)
                        )
                    )
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = DarkNavy,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // --- Model Selection Bottom Sheet ---
    if (showModelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelSheet = false },
            containerColor = CardBg,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "SELECT GEMINI MODEL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = SlateDim,
                    letterSpacing = 1.5.sp
                )

                ModelSelectionCard(
                    modelId = "gemini-3.5-flash",
                    name = "Gemini 3.5 Flash",
                    tag = "Default • General Tasks",
                    description = "Balanced high speed, high reasoning with Google Search & Maps Grounding.",
                    isSelected = selectedModel == "gemini-3.5-flash",
                    color = CyanColor,
                    onClick = {
                        viewModel.setSelectedModel("gemini-3.5-flash")
                        showModelSheet = false
                    }
                )

                ModelSelectionCard(
                    modelId = "gemini-3.1-pro-preview",
                    name = "Gemini 3.1 Pro",
                    tag = "Complex Reasoning & Code",
                    description = "State-of-the-art model for particularly complex math, coding, logic & deep architecture.",
                    isSelected = selectedModel == "gemini-3.1-pro-preview",
                    color = PurpleColor,
                    onClick = {
                        viewModel.setSelectedModel("gemini-3.1-pro-preview")
                        showModelSheet = false
                    }
                )

                ModelSelectionCard(
                    modelId = "gemini-3.1-flash-lite-preview",
                    name = "Gemini 3.1 Flash-Lite",
                    tag = "Lightning Speed",
                    description = "Ultra-fast lightweight model for tasks that must happen with lowest latency.",
                    isSelected = selectedModel == "gemini-3.1-flash-lite-preview",
                    color = AmberColor,
                    onClick = {
                        viewModel.setSelectedModel("gemini-3.1-flash-lite-preview")
                        showModelSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- Persona Selection Bottom Sheet ---
    if (showPersonaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPersonaSheet = false },
            containerColor = CardBg,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SELECT CHATBOT ROLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = SlateDim,
                    letterSpacing = 1.5.sp
                )

                ChatPersona.values().forEach { persona ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selectedPersona == persona) PurpleColor.copy(alpha = 0.15f) else DarkNavy)
                            .border(
                                1.dp,
                                if (selectedPersona == persona) PurpleColor else CardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                viewModel.setChatPersona(persona)
                                showPersonaSheet = false
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = persona.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedPersona == persona) PurpleColor else SlateBright
                                )
                                Text(
                                    text = persona.description,
                                    fontSize = 12.sp,
                                    color = SlateDim
                                )
                                Text(
                                    text = "Default Model: ${persona.defaultModel}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CyanColor
                                )
                            }

                            if (selectedPersona == persona) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = PurpleColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ModelSelectionCard(
    modelId: String,
    name: String,
    tag: String,
    description: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else DarkNavy)
            .border(
                1.dp,
                if (isSelected) color else CardBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) color else SlateBright
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = SlateDim
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: JarvisViewModel.ChatbotUiMessage,
    onOpenUrl: (String) -> Unit,
    onCopyText: (String) -> Unit
) {
    val isUser = message.sender == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CyanColor)
                )
                Text(
                    text = message.modelUsed ?: "Gemini 3.5 Flash",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanColor
                )
                if (message.persona != null) {
                    Text(
                        text = "• ${message.persona.title}",
                        fontSize = 10.sp,
                        color = SlateDim
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
                    } else {
                        Brush.linearGradient(listOf(CardBg, Color(0xFF1E293B)))
                    }
                )
                .border(
                    1.dp,
                    if (isUser) CyanColor.copy(alpha = 0.4f) else CardBorder,
                    RoundedCornerShape(18.dp)
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = message.message,
                    color = SlateBright,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Grounding Citations (Google Search & Google Maps)
                if (message.citations.isNotEmpty()) {
                    Divider(color = CardBorder, thickness = 1.dp)
                    Text(
                        text = "GROUNDED SOURCES:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = SlateDim,
                        letterSpacing = 1.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        message.citations.forEach { citation ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (citation.isMapPlace) AmberColor.copy(alpha = 0.1f) else CyanColor.copy(alpha = 0.1f))
                                    .border(
                                        1.dp,
                                        if (citation.isMapPlace) AmberColor.copy(alpha = 0.4f) else CyanColor.copy(alpha = 0.4f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onOpenUrl(citation.url) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (citation.isMapPlace) Icons.Default.Place else Icons.Default.Language,
                                        contentDescription = null,
                                        tint = if (citation.isMapPlace) AmberColor else CyanColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = citation.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (citation.isMapPlace) AmberColor else CyanColor,
                                            maxLines = 1
                                        )
                                        if (!citation.address.isNullOrBlank()) {
                                            Text(
                                                text = citation.address,
                                                fontSize = 9.sp,
                                                color = SlateDim,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open",
                                        tint = SlateDim,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Copy Action Button
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onCopyText(message.message) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = SlateDim,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator(modelName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = CyanColor,
            strokeWidth = 2.dp
        )
        Text(
            text = modelName,
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic,
            color = SlateDim
        )
    }
}
