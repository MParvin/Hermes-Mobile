package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.MessageEntity
import com.example.data.model.AgentPersonality
import com.example.data.model.ModelProviderType
import com.example.ui.components.HighRiskApprovalCard
import com.example.ui.components.ThoughtCollapseCard
import com.example.ui.components.ToolExecutionCard
import com.example.ui.theme.HermesAmber
import com.example.ui.theme.HermesCyan
import com.example.ui.theme.HermesGreen
import com.example.ui.theme.HermesRed
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.HermesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isBusy by viewModel.isAgentBusy.collectAsState()
    val activePersonality by viewModel.activePersonality.collectAsState()
    val selectedModelProvider by viewModel.selectedModelProvider.collectAsState()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var showPersonalitySheet by remember { mutableStateOf(false) }

    // Speech-to-text recognizer launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .imePadding()
    ) {
        // Quick Action Chips & Model Badge Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = { showPersonalitySheet = true },
                label = { Text("⚡ ${selectedModelProvider.displayName} | ${activePersonality.name}", color = HermesAmber, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(containerColor = Slate950)
            )

            QuickActionChip("🌅 Briefing") { viewModel.sendMessage("Execute morning briefing skill") }
            QuickActionChip("🔋 Telemetry") { viewModel.sendMessage("Inspect device telemetry and battery") }
            QuickActionChip("📩 Read SMS") { viewModel.sendMessage("Read my 5 most recent SMS messages") }
            QuickActionChip("📍 GPS") { viewModel.sendMessage("What is my current device location coordinates?") }
            QuickActionChip("🌐 Web Search") { viewModel.sendMessage("Search web for latest technology breakthroughs") }
        }

        // Messages List
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                EmptyChatGreeting(onPromptClick = { prompt ->
                    viewModel.sendMessage(prompt)
                })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(6.dp)) }
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            onApproveTool = { params ->
                                val toolName = extractToolName(msg.toolCallJson)
                                viewModel.approveToolExecution(msg.id, toolName, params)
                            },
                            onRejectTool = {
                                val toolName = extractToolName(msg.toolCallJson)
                                viewModel.rejectToolExecution(msg.id, toolName)
                            },
                            onSpeak = { viewModel.speakText(msg.content) }
                        )
                    }
                    if (isBusy) {
                        item {
                            AgentThinkingIndicator()
                        }
                    }
                    item { Spacer(modifier = Modifier.height(10.dp)) }
                }
            }
        }

        // Input Field Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Slate900,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Dictation Button
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Hermes Agent...")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Voice Input",
                        tint = HermesCyan
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Instruct Hermes (e.g., 'Check SMS', '/goal audit disk')",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedBorderColor = HermesAmber,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 4
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isBusy) {
                            viewModel.sendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isBusy,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isBusy) HermesAmber else Slate800)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isBusy) Slate950 else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Personality & Model Selection BottomSheet
    if (showPersonalitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showPersonalitySheet = false },
            containerColor = Slate900,
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Select Agent Persona & Model Provider",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HermesAmber)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ACTIVE PERSONALITY",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                )
                Spacer(modifier = Modifier.height(6.dp))

                AgentPersonality.DEFAULT_PERSONALITIES.forEach { p ->
                    val isSelected = p.id == activePersonality.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.setPersonality(p)
                                showPersonalitySheet = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Slate800 else Slate950
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) HermesAmber else Slate700)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) HermesAmber else Color.White
                                )
                            )
                            Text(
                                text = p.tagline,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ACTIVE MODEL BACKEND",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                )
                Spacer(modifier = Modifier.height(6.dp))

                ModelProviderType.values().forEach { mp ->
                    val isSelected = mp == selectedModelProvider
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                viewModel.setModelProvider(mp)
                                showPersonalitySheet = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Slate800 else Slate950
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) HermesCyan else Slate700)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = mp.displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) HermesCyan else Color.White
                                    )
                                )
                                Text(
                                    text = mp.defaultModel,
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                )
                            }
                            if (isSelected) {
                                Text("ACTIVE", style = MaterialTheme.typography.labelSmall.copy(color = HermesCyan, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun QuickActionChip(label: String, onClick: () -> Unit) {
    Surface(
        color = Slate950,
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate700)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0)),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun EmptyChatGreeting(onPromptClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = Slate900,
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("⚡", fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "HERMES AUTONOMOUS AGENT",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = HermesAmber
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "On-device intelligence with OS-level tool-calling, persistence, and self-hosted gateway connectivity.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TRY AN AUTONOMOUS CAPABILITY",
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), letterSpacing = 1.sp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        val samplePrompts = listOf(
            "Inspect my device telemetry, battery rate, and available storage.",
            "Read my 5 most recent SMS messages and summarize them.",
            "Run a morning briefing skill with calendar and news.",
            "Search the web for the latest Gemini 2.5 architecture details."
        )

        samplePrompts.forEach { prompt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onPromptClick(prompt) },
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate800))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = HermesAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: MessageEntity,
    onApproveTool: (Map<String, Any?>) -> Unit,
    onRejectTool: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.sender == "user"
    val isSystem = message.sender == "system"
    val isTool = message.sender == "tool"

    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender header badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = when {
                    isUser -> "YOU"
                    isSystem -> "SYSTEM PROTOCOL"
                    isTool -> "TOOL EXECUTION"
                    else -> "HERMES (${message.modelBadge ?: "Autonomous"})"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isUser -> Color(0xFF94A3B8)
                        isSystem -> HermesRed
                        isTool -> HermesCyan
                        else -> HermesAmber
                    }
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = timeFormatted, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF475569)))
        }

        // Thought card if present
        if (!message.thoughts.isNullOrBlank()) {
            ThoughtCollapseCard(thoughts = message.thoughts)
        }

        // High risk approval card if pending
        if (!message.pendingApprovalId.isNullOrBlank() && !message.toolCallJson.isNullOrBlank()) {
            val toolName = extractToolName(message.toolCallJson)
            HighRiskApprovalCard(
                toolName = toolName,
                paramsJson = message.toolCallJson,
                messageId = message.id,
                onApprove = onApproveTool,
                onReject = onRejectTool
            )
        } else if (!message.toolCallJson.isNullOrBlank() || !message.toolResultJson.isNullOrBlank()) {
            // Tool execution card
            val toolName = extractToolName(message.toolCallJson)
            ToolExecutionCard(
                toolName = toolName,
                toolCallJson = message.toolCallJson,
                toolResultJson = message.toolResultJson
            )
        }

        // Main message content bubble
        if (message.content.isNotBlank()) {
            Card(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .padding(vertical = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isUser -> Color(0xFF1E293B)
                        isSystem -> Color(0xFF261010)
                        isTool -> Color(0xFF0F2027)
                        else -> Slate900
                    }
                ),
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isUser) 14.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 14.dp
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(
                        when {
                            isUser -> Color(0xFF334155)
                            isSystem -> HermesRed
                            isTool -> HermesCyan
                            else -> Slate800
                        }
                    )
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFF1F5F9),
                            lineHeight = 20.sp
                        )
                    )

                    // Audio playback icon for assistant responses
                    if (!isUser && !isSystem) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = onSpeak,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Read aloud",
                                    tint = HermesCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentThinkingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Slate900, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = HermesAmber
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Hermes is reasoning & executing tools...",
            style = MaterialTheme.typography.labelSmall.copy(color = HermesAmber, fontFamily = FontFamily.Monospace)
        )
    }
}

private fun extractToolName(toolCallJson: String?): String {
    if (toolCallJson.isNullOrBlank()) return "system_tool"
    return try {
        org.json.JSONObject(toolCallJson).optString("name", "system_tool")
    } catch (_: Exception) {
        "system_tool"
    }
}
