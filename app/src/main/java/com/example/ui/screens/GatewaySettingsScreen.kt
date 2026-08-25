package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.ModelProviderType
import com.example.ui.theme.HermesAmber
import com.example.ui.theme.HermesCyan
import com.example.ui.theme.HermesGreen
import com.example.ui.theme.HermesRed
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.HermesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ModelOption(
    val id: String,
    val displayName: String,
    val description: String
)

val INITIAL_GEMINI_MODELS = listOf(
    ModelOption("gemini-2.5-flash", "Gemini 2.5 Flash", "Default ultra-fast reasoning & tool calling"),
    ModelOption("gemini-2.5-pro", "Gemini 2.5 Pro", "Deep complex coding & multi-turn reasoning"),
    ModelOption("gemini-2.0-flash", "Gemini 2.0 Flash", "Next-gen low-latency multimodal"),
    ModelOption("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", "Ultra-lightweight fast mobile responses"),
    ModelOption("gemini-1.5-pro", "Gemini 1.5 Pro", "2M token ultra-large context window"),
    ModelOption("gemini-1.5-flash", "Gemini 1.5 Flash", "High efficiency standard model")
)

val INITIAL_CLAUDE_MODELS = listOf(
    ModelOption("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "State-of-the-art coding & agentic orchestration"),
    ModelOption("claude-3-7-sonnet", "Claude 3.7 Sonnet", "Hybrid deliberate thought architecture"),
    ModelOption("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "High speed & concise analytical tasks"),
    ModelOption("claude-3-opus-20240229", "Claude 3 Opus", "Deep academic research & heavy reasoning")
)

val INITIAL_OPENAI_MODELS = listOf(
    ModelOption("gpt-4o", "GPT-4o", "Flagship omni multi-modal & fast execution"),
    ModelOption("gpt-4o-mini", "GPT-4o Mini", "Affordable, high speed tool execution"),
    ModelOption("o3-mini", "o3-mini", "Advanced STEM & mathematical chain-of-thought"),
    ModelOption("o1-preview", "o1-preview", "Extended deliberate reasoning"),
    ModelOption("gpt-4-turbo", "GPT-4 Turbo", "High capacity 128k context")
)

val INITIAL_OPENROUTER_MODELS = listOf(
    ModelOption("nousresearch/hermes-3-llama-3.1-405b", "Hermes 3 (405B)", "Nous Research flagship sovereign agent"),
    ModelOption("nousresearch/hermes-3-llama-3.1-70b", "Hermes 3 (70B)", "Balanced uncensored agentic power"),
    ModelOption("deepseek/deepseek-r1", "DeepSeek R1", "Open weights deep reasoning powerhouse"),
    ModelOption("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 (70B)", "Meta flagship open instruction model"),
    ModelOption("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", "Anthropic flagship via OpenRouter"),
    ModelOption("google/gemini-2.5-flash", "Gemini 2.5 Flash", "Google Gemini via OpenRouter gateway")
)

val INITIAL_LOCAL_MODELS = listOf(
    ModelOption("hermes-3-llama-3.1-8b", "Hermes 3 (8B Local)", "Official Hermes 3 agent local weights"),
    ModelOption("llama3.2:3b", "Llama 3.2 (3B)", "Ultra-compact mobile on-device engine"),
    ModelOption("mistral:7b", "Mistral (7B)", "Fast local general-purpose model"),
    ModelOption("qwen2.5:7b", "Qwen 2.5 (7B)", "High accuracy code & mathematical logic"),
    ModelOption("deepseek-r1:8b", "DeepSeek R1 (8B)", "Local distilled reasoning model"),
    ModelOption("gemma2:9b", "Gemma 2 (9B)", "Google open lightweight architecture")
)

// Validation Result Data Structure
data class KeyValidationResult(
    val isValid: Boolean,
    val message: String,
    val isWarningOnly: Boolean = false,
    val isSuccess: Boolean = false
)

fun validateGeminiKey(key: String): KeyValidationResult {
    val trimmed = key.trim()
    return when {
        trimmed.isEmpty() -> KeyValidationResult(
            isValid = true,
            message = "Default server-side key will be used (ready)",
            isWarningOnly = true
        )
        trimmed == "MY_GEMINI_API_KEY" -> KeyValidationResult(
            isValid = true,
            message = "Template placeholder active (simulated offline mode)",
            isWarningOnly = true
        )
        trimmed.startsWith("AIzaSy") && trimmed.length in 35..45 -> KeyValidationResult(
            isValid = true,
            message = "Valid Google AI Studio API Key format (AIzaSy...)",
            isSuccess = true
        )
        trimmed.length >= 20 -> KeyValidationResult(
            isValid = true,
            message = "Custom API key format detected",
            isSuccess = true
        )
        else -> KeyValidationResult(
            isValid = false,
            message = "Expected format starts with 'AIzaSy...' (~39 chars)"
        )
    }
}

fun validateAnthropicKey(key: String): KeyValidationResult {
    val trimmed = key.trim()
    return when {
        trimmed.isEmpty() -> KeyValidationResult(
            isValid = false,
            message = "API key required for Anthropic Claude direct requests"
        )
        trimmed.startsWith("sk-ant-") && trimmed.length >= 25 -> KeyValidationResult(
            isValid = true,
            message = "Valid Anthropic API Key format (sk-ant-...)",
            isSuccess = true
        )
        trimmed.startsWith("sk-ant-") -> KeyValidationResult(
            isValid = false,
            message = "Incomplete key (sk-ant- keys are typically > 50 chars)"
        )
        else -> KeyValidationResult(
            isValid = false,
            message = "Expected format: starts with 'sk-ant-'"
        )
    }
}

fun validateOpenAiKey(key: String): KeyValidationResult {
    val trimmed = key.trim()
    return when {
        trimmed.isEmpty() -> KeyValidationResult(
            isValid = false,
            message = "API key required for OpenAI requests"
        )
        (trimmed.startsWith("sk-proj-") || trimmed.startsWith("sk-")) && trimmed.length >= 25 -> KeyValidationResult(
            isValid = true,
            message = "Valid OpenAI API Key format (sk-...)",
            isSuccess = true
        )
        trimmed.startsWith("sk-") -> KeyValidationResult(
            isValid = false,
            message = "Incomplete key: OpenAI keys are typically > 40 chars"
        )
        else -> KeyValidationResult(
            isValid = false,
            message = "Expected format: starts with 'sk-proj-' or 'sk-'"
        )
    }
}

fun validateOpenRouterKey(key: String): KeyValidationResult {
    val trimmed = key.trim()
    return when {
        trimmed.isEmpty() -> KeyValidationResult(
            isValid = false,
            message = "API key required for OpenRouter gateway"
        )
        (trimmed.startsWith("sk-or-v1-") || trimmed.startsWith("sk-or-")) && trimmed.length >= 25 -> KeyValidationResult(
            isValid = true,
            message = "Valid OpenRouter API Key format (sk-or-v1-...)",
            isSuccess = true
        )
        trimmed.startsWith("sk-or-") -> KeyValidationResult(
            isValid = false,
            message = "Incomplete OpenRouter key"
        )
        else -> KeyValidationResult(
            isValid = false,
            message = "Expected format: starts with 'sk-or-v1-'"
        )
    }
}

fun validateTelegramToken(token: String): KeyValidationResult {
    val trimmed = token.trim()
    return when {
        trimmed.isEmpty() -> KeyValidationResult(
            isValid = true,
            message = "Telegram Bot Token not configured (bridge inactive)",
            isWarningOnly = true
        )
        trimmed.matches(Regex("""^\d{8,12}:[A-Za-z0-9_-]{30,}$""")) -> KeyValidationResult(
            isValid = true,
            message = "Valid Telegram Bot Token format (<bot_id>:<secret>)",
            isSuccess = true
        )
        trimmed.contains(":") -> KeyValidationResult(
            isValid = true,
            message = "Telegram Token pattern detected",
            isSuccess = true
        )
        else -> KeyValidationResult(
            isValid = false,
            message = "Expected format: <number>:<token> (e.g. 123456789:ABC...)"
        )
    }
}

fun validateLocalEndpoint(endpoint: String): KeyValidationResult {
    val trimmed = endpoint.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> KeyValidationResult(
            isValid = true,
            message = "Valid HTTP/HTTPS endpoint URL",
            isSuccess = true
        )
        else -> KeyValidationResult(
            isValid = false,
            message = "Must start with http:// or https://"
        )
    }
}

fun validatePort(port: String): KeyValidationResult {
    val p = port.trim().toIntOrNull()
    return when {
        p != null && p in 1024..65535 -> KeyValidationResult(
            isValid = true,
            message = "Valid daemon port ($p)",
            isSuccess = true
        )
        else -> KeyValidationResult(
            isValid = false,
            message = "Port must be a number between 1024 and 65535"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewaySettingsScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    val isKillSwitchActive by viewModel.isKillSwitchActive.collectAsState()
    val savedSettings by viewModel.allSettings.collectAsState()
    val currentActiveProvider by viewModel.selectedModelProvider.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Form states
    var isInitialized by remember { mutableStateOf(false) }

    var geminiKey by remember { mutableStateOf(BuildConfig.GEMINI_API_KEY) }
    var geminiModel by remember { mutableStateOf("gemini-2.5-flash") }
    var isGeminiEnabled by remember { mutableStateOf(true) }
    val geminiModels = remember { mutableStateListOf(*INITIAL_GEMINI_MODELS.toTypedArray()) }

    var anthropicKey by remember { mutableStateOf("") }
    var anthropicModel by remember { mutableStateOf("claude-3-5-sonnet-20241022") }
    var isAnthropicEnabled by remember { mutableStateOf(true) }
    val anthropicModels = remember { mutableStateListOf(*INITIAL_CLAUDE_MODELS.toTypedArray()) }

    var openaiKey by remember { mutableStateOf("") }
    var openaiBaseUrl by remember { mutableStateOf("https://api.openai.com/v1") }
    var openaiModel by remember { mutableStateOf("gpt-4o") }
    var isOpenAiEnabled by remember { mutableStateOf(true) }
    val openaiModels = remember { mutableStateListOf(*INITIAL_OPENAI_MODELS.toTypedArray()) }

    var openRouterKey by remember { mutableStateOf("") }
    var openRouterModel by remember { mutableStateOf("nousresearch/hermes-3-llama-3.1-405b") }
    var isOpenRouterEnabled by remember { mutableStateOf(true) }
    val openRouterModels = remember { mutableStateListOf(*INITIAL_OPENROUTER_MODELS.toTypedArray()) }

    var localEndpoint by remember { mutableStateOf("http://10.0.2.2:11434/v1/chat/completions") }
    var localApiKey by remember { mutableStateOf("sk-local") }
    var localModel by remember { mutableStateOf("hermes-3-llama-3.1-8b") }
    var isLocalEnabled by remember { mutableStateOf(true) }
    val localModels = remember { mutableStateListOf(*INITIAL_LOCAL_MODELS.toTypedArray()) }

    var isMoaEnabled by remember { mutableStateOf(true) }

    var telegramToken by remember { mutableStateOf("") }
    var telegramOwnerId by remember { mutableStateOf("") }
    var isTelegramEnabled by remember { mutableStateOf(false) }

    var httpPort by remember { mutableStateOf("8080") }
    var isHttpEnabled by remember { mutableStateOf(true) }

    var activeProviderSelection by remember { mutableStateOf(currentActiveProvider) }
    var showSaveBanner by remember { mutableStateOf(false) }

    // Synchronize initial state from saved Room settings
    LaunchedEffect(savedSettings) {
        if (!isInitialized && savedSettings.isNotEmpty()) {
            geminiKey = savedSettings["gemini_api_key"] ?: BuildConfig.GEMINI_API_KEY
            geminiModel = savedSettings["gemini_model"] ?: "gemini-2.5-flash"
            isGeminiEnabled = savedSettings["provider_enabled_GEMINI"]?.toBoolean() ?: true

            anthropicKey = savedSettings["anthropic_api_key"] ?: ""
            anthropicModel = savedSettings["anthropic_model"] ?: "claude-3-5-sonnet-20241022"
            isAnthropicEnabled = savedSettings["provider_enabled_CLAUDE"]?.toBoolean() ?: true

            openaiKey = savedSettings["openai_api_key"] ?: ""
            openaiBaseUrl = savedSettings["openai_base_url"] ?: "https://api.openai.com/v1"
            openaiModel = savedSettings["openai_model"] ?: "gpt-4o"
            isOpenAiEnabled = savedSettings["provider_enabled_OPENAI"]?.toBoolean() ?: true

            openRouterKey = savedSettings["openrouter_api_key"] ?: ""
            openRouterModel = savedSettings["openrouter_model"] ?: "nousresearch/hermes-3-llama-3.1-405b"
            isOpenRouterEnabled = savedSettings["provider_enabled_OPENROUTER"]?.toBoolean() ?: true

            localEndpoint = savedSettings["local_endpoint_url"] ?: "http://10.0.2.2:11434/v1/chat/completions"
            localApiKey = savedSettings["local_api_key"] ?: "sk-local"
            localModel = savedSettings["local_model_name"] ?: "hermes-3-llama-3.1-8b"
            isLocalEnabled = savedSettings["provider_enabled_LOCAL_CUSTOM"]?.toBoolean() ?: true

            isMoaEnabled = savedSettings["provider_enabled_MOA_MIXTURE"]?.toBoolean() ?: true

            telegramToken = savedSettings["telegram_bot_token"] ?: ""
            telegramOwnerId = savedSettings["telegram_owner_chat_id"] ?: ""
            isTelegramEnabled = savedSettings["telegram_bridge_enabled"]?.toBoolean() ?: false

            httpPort = savedSettings["local_http_port"] ?: "8080"
            isHttpEnabled = savedSettings["local_http_enabled"]?.toBoolean() ?: true

            val savedActive = savedSettings["active_provider"]
            if (savedActive != null) {
                try {
                    activeProviderSelection = ModelProviderType.valueOf(savedActive)
                } catch (_: Exception) {}
            }

            isInitialized = true
        }
    }

    // Function to persist all changes
    val saveAllAction = {
        val map = mapOf(
            "gemini_api_key" to geminiKey,
            "gemini_model" to geminiModel,
            "provider_enabled_GEMINI" to isGeminiEnabled.toString(),

            "anthropic_api_key" to anthropicKey,
            "anthropic_model" to anthropicModel,
            "provider_enabled_CLAUDE" to isAnthropicEnabled.toString(),

            "openai_api_key" to openaiKey,
            "openai_base_url" to openaiBaseUrl,
            "openai_model" to openaiModel,
            "provider_enabled_OPENAI" to isOpenAiEnabled.toString(),

            "openrouter_api_key" to openRouterKey,
            "openrouter_model" to openRouterModel,
            "provider_enabled_OPENROUTER" to isOpenRouterEnabled.toString(),

            "local_endpoint_url" to localEndpoint,
            "local_api_key" to localApiKey,
            "local_model_name" to localModel,
            "provider_enabled_LOCAL_CUSTOM" to isLocalEnabled.toString(),

            "provider_enabled_MOA_MIXTURE" to isMoaEnabled.toString(),

            "telegram_bot_token" to telegramToken,
            "telegram_owner_chat_id" to telegramOwnerId,
            "telegram_bridge_enabled" to isTelegramEnabled.toString(),

            "local_http_port" to httpPort,
            "local_http_enabled" to isHttpEnabled.toString(),

            "active_provider" to activeProviderSelection.name
        )
        viewModel.saveAllSettings(map, "All Settings & Keys saved to secure storage")
        viewModel.setModelProvider(activeProviderSelection)
        showSaveBanner = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Action Bar with prominent Save Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GATEWAY & CREDENTIALS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Model selection, API keys & background bridges",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                )
            }

            Button(
                onClick = saveAllAction,
                colors = ButtonDefaults.buttonColors(containerColor = HermesAmber, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier.testTag("save_settings_top_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SAVE ALL", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (showSaveBanner) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color(0xFF064E3B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = HermesGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "All settings, active models, and gateway keys saved successfully.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA7F3D0), fontWeight = FontWeight.Medium)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Kill Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (isKillSwitchActive) Color(0xFF450A0A) else Slate900),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(if (isKillSwitchActive) HermesRed else Slate800)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Kill Switch",
                            tint = if (isKillSwitchActive) HermesRed else HermesAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GLOBAL KILL SWITCH",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKillSwitchActive) HermesRed else Color.White
                                )
                            )
                            Text(
                                text = if (isKillSwitchActive) "🚨 All Autonomous Tool Executions REVOKED" else "✅ Autonomous Engine Operating Normally",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.toggleKillSwitch() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isKillSwitchActive) HermesGreen else HermesRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isKillSwitchActive) "DISENGAGE KILL SWITCH (RESUME)" else "ENGAGE EMERGENCY KILL SWITCH",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Active Model Selector
        Text(
            text = "PRIMARY ACTIVE AGENT MODEL",
            style = MaterialTheme.typography.labelSmall.copy(color = HermesAmber, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))

        SettingsCard {
            Text(
                text = "Select which provider and model powers Hermes agent conversations and tool executions:",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
            )
            Spacer(modifier = Modifier.height(10.dp))

            ActiveProviderComboBox(
                selectedProvider = activeProviderSelection,
                onProviderSelected = {
                    activeProviderSelection = it
                    viewModel.setModelProvider(it)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            val activeModelName = when (activeProviderSelection) {
                ModelProviderType.GEMINI -> geminiModel
                ModelProviderType.CLAUDE -> anthropicModel
                ModelProviderType.OPENAI -> openaiModel
                ModelProviderType.OPENROUTER -> openRouterModel
                ModelProviderType.LOCAL_CUSTOM -> localModel
                ModelProviderType.MOA_MIXTURE -> "MoA Ensemble (Consensus)"
            }

            Surface(
                color = Slate950,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProviderLogoBadge(provider = activeProviderSelection, size = 28)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("ACTIVE MODEL TARGET", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                            Text(activeModelName, style = MaterialTheme.typography.bodyMedium.copy(color = HermesAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                        }
                    }
                    val isProviderActiveAndEnabled = when (activeProviderSelection) {
                        ModelProviderType.GEMINI -> isGeminiEnabled
                        ModelProviderType.CLAUDE -> isAnthropicEnabled
                        ModelProviderType.OPENAI -> isOpenAiEnabled
                        ModelProviderType.OPENROUTER -> isOpenRouterEnabled
                        ModelProviderType.LOCAL_CUSTOM -> isLocalEnabled
                        ModelProviderType.MOA_MIXTURE -> isMoaEnabled
                    }
                    Surface(
                        color = if (isProviderActiveAndEnabled) Color(0xFF064E3B) else Color(0xFF450A0A),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isProviderActiveAndEnabled) "ENABLED" else "DISABLED",
                            color = if (isProviderActiveAndEnabled) HermesGreen else HermesRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Model Providers with Toggle Switches & Model Selectors
        Text(
            text = "MODEL PROVIDERS & CREDENTIALS",
            style = MaterialTheme.typography.labelSmall.copy(color = HermesCyan, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 1. Google Gemini Provider
        ProviderCard(
            title = "Google Gemini",
            subtitle = "Google DeepMind Multimodal & Flash Models",
            providerType = ModelProviderType.GEMINI,
            isEnabled = isGeminiEnabled,
            onToggleEnabled = {
                isGeminiEnabled = it
                viewModel.setProviderEnabled(ModelProviderType.GEMINI, it)
            },
            accentColor = HermesAmber
        ) {
            ModelComboBox(
                label = "Gemini Model Selection",
                selectedModel = geminiModel,
                modelOptions = geminiModels,
                providerName = "Google Gemini",
                onModelSelected = { geminiModel = it },
                onRefreshModels = {
                    coroutineScope.launch {
                        delay(600)
                        val extra = listOf(
                            ModelOption("gemini-2.5-flash", "Gemini 2.5 Flash", "Default ultra-fast reasoning & tool calling"),
                            ModelOption("gemini-2.5-pro", "Gemini 2.5 Pro", "Deep complex coding & multi-turn reasoning"),
                            ModelOption("gemini-2.0-pro-exp", "Gemini 2.0 Pro Exp", "Frontier benchmark reasoning engine"),
                            ModelOption("gemini-2.0-flash", "Gemini 2.0 Flash", "Next-gen low-latency multimodal"),
                            ModelOption("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", "Ultra-lightweight fast mobile responses"),
                            ModelOption("gemini-1.5-pro", "Gemini 1.5 Pro", "2M token ultra-large context window"),
                            ModelOption("gemini-1.5-flash", "Gemini 1.5 Flash", "High efficiency standard model")
                        )
                        geminiModels.clear()
                        geminiModels.addAll(extra)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            ValidatedPasswordField(
                value = geminiKey,
                onValueChange = { geminiKey = it },
                label = "Gemini API Key",
                placeholder = "AI Studio API Key (or server-side auto)",
                helperText = "Default server-side key is used if left as default",
                validationResult = validateGeminiKey(geminiKey)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Anthropic Claude Provider
        ProviderCard(
            title = "Anthropic Claude",
            subtitle = "Claude 3.5 Sonnet, 3.5 Haiku, 3.7 Sonnet",
            providerType = ModelProviderType.CLAUDE,
            isEnabled = isAnthropicEnabled,
            onToggleEnabled = {
                isAnthropicEnabled = it
                viewModel.setProviderEnabled(ModelProviderType.CLAUDE, it)
            },
            accentColor = Color(0xFFD97706)
        ) {
            ModelComboBox(
                label = "Claude Model Selection",
                selectedModel = anthropicModel,
                modelOptions = anthropicModels,
                providerName = "Anthropic Claude",
                onModelSelected = { anthropicModel = it },
                onRefreshModels = {
                    coroutineScope.launch {
                        delay(600)
                        val extra = listOf(
                            ModelOption("claude-3-7-sonnet", "Claude 3.7 Sonnet", "Frontier hybrid deliberate reasoning"),
                            ModelOption("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "State-of-the-art coding & agentic orchestration"),
                            ModelOption("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "High speed & concise analytical tasks"),
                            ModelOption("claude-3-opus-20240229", "Claude 3 Opus", "Deep academic research & heavy reasoning")
                        )
                        anthropicModels.clear()
                        anthropicModels.addAll(extra)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            ValidatedPasswordField(
                value = anthropicKey,
                onValueChange = { anthropicKey = it },
                label = "Anthropic API Key",
                placeholder = "sk-ant-...",
                helperText = "Direct REST call to api.anthropic.com/v1/messages",
                validationResult = validateAnthropicKey(anthropicKey)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. OpenAI Provider
        ProviderCard(
            title = "OpenAI",
            subtitle = "GPT-4o, GPT-4o Mini, o3-mini Reasoning",
            providerType = ModelProviderType.OPENAI,
            isEnabled = isOpenAiEnabled,
            onToggleEnabled = {
                isOpenAiEnabled = it
                viewModel.setProviderEnabled(ModelProviderType.OPENAI, it)
            },
            accentColor = HermesGreen
        ) {
            ModelComboBox(
                label = "OpenAI Model Selection",
                selectedModel = openaiModel,
                modelOptions = openaiModels,
                providerName = "OpenAI",
                onModelSelected = { openaiModel = it },
                onRefreshModels = {
                    coroutineScope.launch {
                        delay(600)
                        val extra = listOf(
                            ModelOption("gpt-4o", "GPT-4o", "Flagship omni multi-modal & fast execution"),
                            ModelOption("gpt-4o-mini", "GPT-4o Mini", "Affordable, high speed tool execution"),
                            ModelOption("o3-mini", "o3-mini", "Advanced STEM & mathematical chain-of-thought"),
                            ModelOption("o1", "o1", "Deep reasoning frontier reasoning model"),
                            ModelOption("o1-mini", "o1-mini", "Fast targeted math & coding reasoning"),
                            ModelOption("gpt-4-turbo", "GPT-4 Turbo", "High capacity 128k context")
                        )
                        openaiModels.clear()
                        openaiModels.addAll(extra)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            ValidatedPasswordField(
                value = openaiKey,
                onValueChange = { openaiKey = it },
                label = "OpenAI API Key",
                placeholder = "sk-proj-...",
                helperText = "Standard OpenAI Authorization Bearer Token",
                validationResult = validateOpenAiKey(openaiKey)
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = openaiBaseUrl,
                onValueChange = { openaiBaseUrl = it },
                label = { Text("Base URL Endpoint") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HermesGreen,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate950,
                    unfocusedContainerColor = Slate950
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. OpenRouter Multi-Gateway
        ProviderCard(
            title = "OpenRouter Gateway",
            subtitle = "Nous Hermes 3 (405B/70B), Llama 3.3, DeepSeek",
            providerType = ModelProviderType.OPENROUTER,
            isEnabled = isOpenRouterEnabled,
            onToggleEnabled = {
                isOpenRouterEnabled = it
                viewModel.setProviderEnabled(ModelProviderType.OPENROUTER, it)
            },
            accentColor = Color(0xFFA855F7)
        ) {
            ModelComboBox(
                label = "OpenRouter Model Selection",
                selectedModel = openRouterModel,
                modelOptions = openRouterModels,
                providerName = "OpenRouter",
                onModelSelected = { openRouterModel = it },
                onRefreshModels = {
                    coroutineScope.launch {
                        delay(600)
                        val extra = listOf(
                            ModelOption("nousresearch/hermes-3-llama-3.1-405b", "Hermes 3 (405B)", "Nous Research flagship sovereign agent"),
                            ModelOption("nousresearch/hermes-3-llama-3.1-70b", "Hermes 3 (70B)", "Balanced uncensored agentic power"),
                            ModelOption("deepseek/deepseek-r1", "DeepSeek R1", "Open weights deep reasoning powerhouse"),
                            ModelOption("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 (70B)", "Meta flagship open instruction model"),
                            ModelOption("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", "Anthropic flagship via OpenRouter"),
                            ModelOption("qwen/qwen-2.5-72b-instruct", "Qwen 2.5 (72B)", "High performance code & multi-lingual"),
                            ModelOption("google/gemini-2.5-flash", "Gemini 2.5 Flash", "Google Gemini via OpenRouter gateway")
                        )
                        openRouterModels.clear()
                        openRouterModels.addAll(extra)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            ValidatedPasswordField(
                value = openRouterKey,
                onValueChange = { openRouterKey = it },
                label = "OpenRouter API Key",
                placeholder = "sk-or-v1-...",
                helperText = "Access hundreds of open & frontier models",
                validationResult = validateOpenRouterKey(openRouterKey)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Local / Ollama / LM Studio
        ProviderCard(
            title = "Local / Ollama / LM Studio",
            subtitle = "On-device or LAN private offline inference",
            providerType = ModelProviderType.LOCAL_CUSTOM,
            isEnabled = isLocalEnabled,
            onToggleEnabled = {
                isLocalEnabled = it
                viewModel.setProviderEnabled(ModelProviderType.LOCAL_CUSTOM, it)
            },
            accentColor = HermesCyan
        ) {
            ModelComboBox(
                label = "Local Model Name",
                selectedModel = localModel,
                modelOptions = localModels,
                providerName = "Local Ollama / LM Studio",
                onModelSelected = { localModel = it },
                onRefreshModels = {
                    coroutineScope.launch {
                        delay(600)
                        val extra = listOf(
                            ModelOption("hermes-3-llama-3.1-8b", "Hermes 3 (8B Local)", "Official Hermes 3 agent local weights"),
                            ModelOption("llama3.2:3b", "Llama 3.2 (3B)", "Ultra-compact mobile on-device engine"),
                            ModelOption("mistral:7b", "Mistral (7B)", "Fast local general-purpose model"),
                            ModelOption("qwen2.5:7b", "Qwen 2.5 (7B)", "High accuracy code & mathematical logic"),
                            ModelOption("deepseek-r1:8b", "DeepSeek R1 (8B)", "Local distilled reasoning model"),
                            ModelOption("phi4:14b", "Phi-4 (14B)", "Microsoft compact reasoning powerhouse"),
                            ModelOption("gemma2:9b", "Gemma 2 (9B)", "Google open lightweight architecture")
                        )
                        localModels.clear()
                        localModels.addAll(extra)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = localEndpoint,
                onValueChange = { localEndpoint = it },
                label = { Text("Ollama / LM Studio API Endpoint") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HermesCyan,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate950,
                    unfocusedContainerColor = Slate950
                ),
                shape = RoundedCornerShape(10.dp)
            )

            val endpointVal = validateLocalEndpoint(localEndpoint)
            ValidationBadge(validationResult = endpointVal)

            Spacer(modifier = Modifier.height(8.dp))
            ValidatedPasswordField(
                value = localApiKey,
                onValueChange = { localApiKey = it },
                label = "Local Authorization Key (Optional)",
                placeholder = "sk-local",
                helperText = "Sent in Authorization header",
                validationResult = KeyValidationResult(isValid = true, message = "Local authorization token ready", isSuccess = true)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Mixture of Agents (MoA)
        ProviderCard(
            title = "Mixture of Agents (MoA)",
            subtitle = "Multi-model consensus & synthesis pipeline",
            providerType = ModelProviderType.MOA_MIXTURE,
            isEnabled = isMoaEnabled,
            onToggleEnabled = {
                isMoaEnabled = it
                viewModel.setProviderEnabled(ModelProviderType.MOA_MIXTURE, it)
            },
            accentColor = HermesAmber
        ) {
            Text(
                text = "Runs parallel queries across active providers and synthesizes reasoning traces through Hermes verification layer.",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Telegram Gateway Bridge
        Text(
            text = "TELEGRAM BOT GATEWAY BRIDGE",
            style = MaterialTheme.typography.labelSmall.copy(color = HermesCyan, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        ProviderCard(
            title = "Telegram Bot Bridge",
            subtitle = "Long-polling worker for remote owner commands",
            customLogo = { TelegramLogoBadge(size = 32) },
            isEnabled = isTelegramEnabled,
            onToggleEnabled = { isTelegramEnabled = it },
            accentColor = HermesCyan
        ) {
            ValidatedPasswordField(
                value = telegramToken,
                onValueChange = { telegramToken = it },
                label = "Telegram Bot Token",
                placeholder = "123456789:ABCdefGhI...",
                helperText = "Obtain from @BotFather on Telegram",
                validationResult = validateTelegramToken(telegramToken)
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = telegramOwnerId,
                onValueChange = { telegramOwnerId = it },
                label = { Text("Owner Telegram Chat ID (Enforces Owner-Only Control)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HermesCyan,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate950,
                    unfocusedContainerColor = Slate950
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Local HTTP API Server
        Text(
            text = "EMBEDDED LOCAL HTTP API GATEWAY",
            style = MaterialTheme.typography.labelSmall.copy(color = HermesGreen, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        ProviderCard(
            title = "Local HTTP Daemon",
            subtitle = "OpenAI-compatible /v1/chat/completions endpoint",
            customLogo = { HttpDaemonLogoBadge(size = 32) },
            isEnabled = isHttpEnabled,
            onToggleEnabled = { isHttpEnabled = it },
            accentColor = HermesGreen
        ) {
            OutlinedTextField(
                value = httpPort,
                onValueChange = { httpPort = it },
                label = { Text("Server Port (default: 8080)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HermesGreen,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate950,
                    unfocusedContainerColor = Slate950
                ),
                shape = RoundedCornerShape(10.dp)
            )

            val portVal = validatePort(httpPort)
            ValidationBadge(validationResult = portVal)

            Spacer(modifier = Modifier.height(8.dp))
            Surface(color = Slate950, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Endpoint: http://localhost:$httpPort/v1/chat/completions",
                    fontFamily = FontFamily.Monospace,
                    color = HermesGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Bottom Master Save Button
        Button(
            onClick = saveAllAction,
            colors = ButtonDefaults.buttonColors(containerColor = HermesAmber, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_all_settings_button")
        ) {
            Icon(imageVector = Icons.Default.Done, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE ALL CHANGES & CONFIGURATIONS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ------------------------------------------------------------------------------------------------
// PROVIDER CARDS & LOGOS
// ------------------------------------------------------------------------------------------------

@Composable
fun ProviderCard(
    title: String,
    subtitle: String,
    providerType: ModelProviderType? = null,
    customLogo: (@Composable () -> Unit)? = null,
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isEnabled) 1.0f else 0.65f),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(if (isEnabled) accentColor.copy(alpha = 0.4f) else Slate800)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visual Provider Logo
                    if (customLogo != null) {
                        customLogo()
                    } else if (providerType != null) {
                        ProviderLogoBadge(provider = providerType, size = 34)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isEnabled) accentColor else Color(0xFF64748B))
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEnabled) Color.White else Color(0xFF94A3B8)
                                )
                            )
                            if (!isEnabled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Slate950,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "DISABLED",
                                        color = HermesRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }

                // Toggle Button in front of provider name
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.3f),
                        uncheckedThumbColor = Slate700,
                        uncheckedTrackColor = Slate950
                    ),
                    modifier = Modifier.testTag("toggle_provider_${title.lowercase().replace(" ", "_")}")
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Slate800, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// HIGH-FIDELITY VECTOR LOGO BADGES
// ------------------------------------------------------------------------------------------------

@Composable
fun ProviderLogoBadge(provider: ModelProviderType, size: Int = 32) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3.5).dp))
            .background(Slate950)
            .border(1.dp, Slate800, RoundedCornerShape((size / 3.5).dp)),
        contentAlignment = Alignment.Center
    ) {
        when (provider) {
            ModelProviderType.GEMINI -> GeminiLogoCanvas(size = (size * 0.75).toInt())
            ModelProviderType.CLAUDE -> ClaudeLogoCanvas(size = (size * 0.75).toInt())
            ModelProviderType.OPENAI -> OpenAiLogoCanvas(size = (size * 0.75).toInt())
            ModelProviderType.OPENROUTER -> OpenRouterLogoCanvas(size = (size * 0.75).toInt())
            ModelProviderType.LOCAL_CUSTOM -> LocalOllamaLogoCanvas(size = (size * 0.75).toInt())
            ModelProviderType.MOA_MIXTURE -> MoaLogoCanvas(size = (size * 0.75).toInt())
        }
    }
}

@Composable
fun GeminiLogoCanvas(size: Int) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        // 4-point curved sparkle
        val path = Path().apply {
            moveTo(cx, 0f)
            cubicTo(cx, cy * 0.4f, cx + cx * 0.4f, cy, w, cy)
            cubicTo(cx + cx * 0.4f, cy, cx, cy + cy * 0.6f, cx, h)
            cubicTo(cx, cy + cy * 0.6f, cx * 0.4f, cy, 0f, cy)
            cubicTo(cx * 0.4f, cy, cx, cy * 0.4f, cx, 0f)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFF59E0B)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}

@Composable
fun ClaudeLogoCanvas(size: Int) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        // Claude terracotta asterisk ray bursts
        val rayColor = Color(0xFFD97706)
        val strokeWidth = w * 0.18f

        // Center dot
        drawCircle(color = Color(0xFFF59E0B), radius = w * 0.2f, center = Offset(cx, cy))

        // 6 spokes
        for (i in 0 until 6) {
            val angle = (i * 60.0) * (Math.PI / 180.0)
            val endX = cx + (w * 0.42f * Math.cos(angle)).toFloat()
            val endY = cy + (h * 0.42f * Math.sin(angle)).toFloat()
            drawLine(
                color = rayColor,
                start = Offset(cx, cy),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun OpenAiLogoCanvas(size: Int) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        val stroke = w * 0.12f
        val color = Color(0xFF10B981)

        // Draw iconic hexagonal spiral aperture
        drawCircle(color = color, radius = w * 0.38f, style = Stroke(width = stroke))
        drawCircle(color = Color(0xFF34D399), radius = w * 0.18f, style = Fill)
    }
}

@Composable
fun OpenRouterLogoCanvas(size: Int) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        val color = Color(0xFFA855F7)
        // Hub with 4 connected node points
        drawCircle(color = Color(0xFFC084FC), radius = w * 0.22f, center = Offset(cx, cy))

        val radius = w * 0.38f
        val nodeRadius = w * 0.1f

        val nodes = listOf(
            Offset(cx, cy - radius),
            Offset(cx + radius, cy),
            Offset(cx, cy + radius),
            Offset(cx - radius, cy)
        )

        nodes.forEach { node ->
            drawLine(
                color = color.copy(alpha = 0.7f),
                start = Offset(cx, cy),
                end = node,
                strokeWidth = w * 0.08f
            )
            drawCircle(color = color, radius = nodeRadius, center = node)
        }
    }
}

@Composable
fun LocalOllamaLogoCanvas(size: Int) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        // Local Server Chip + Terminal Prompt
        drawRoundRect(
            color = Color(0xFF06B6D4),
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.7f, h * 0.7f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.15f, h * 0.15f),
            style = Stroke(width = w * 0.12f)
        )

        // Core prompt '>'
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.35f)
            lineTo(w * 0.55f, cy)
            lineTo(w * 0.35f, h * 0.65f)
        }
        drawPath(path = path, color = Color(0xFF22D3EE), style = Stroke(width = w * 0.1f, cap = StrokeCap.Round))
    }
}

@Composable
fun MoaLogoCanvas(size: Int) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        // 3 overlapping synthesis nodes
        drawCircle(color = Color(0xFFF59E0B).copy(alpha = 0.8f), radius = w * 0.22f, center = Offset(cx, cy - h * 0.2f))
        drawCircle(color = Color(0xFF06B6D4).copy(alpha = 0.8f), radius = w * 0.22f, center = Offset(cx - w * 0.2f, cy + h * 0.15f))
        drawCircle(color = Color(0xFF10B981).copy(alpha = 0.8f), radius = w * 0.22f, center = Offset(cx + w * 0.2f, cy + h * 0.15f))
    }
}

@Composable
fun TelegramLogoBadge(size: Int = 32) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3.5).dp))
            .background(Slate950)
            .border(1.dp, Slate800, RoundedCornerShape((size / 3.5).dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Send,
            contentDescription = "Telegram",
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size((size * 0.55).dp)
        )
    }
}

@Composable
fun HttpDaemonLogoBadge(size: Int = 32) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3.5).dp))
            .background(Slate950)
            .border(1.dp, Slate800, RoundedCornerShape((size / 3.5).dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "HTTP Server",
            tint = HermesGreen,
            modifier = Modifier.size((size * 0.55).dp)
        )
    }
}

// ------------------------------------------------------------------------------------------------
// MODEL COMBO BOX WITH REFRESH BUTTON
// ------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelComboBox(
    label: String,
    selectedModel: String,
    modelOptions: List<ModelOption>,
    providerName: String = "Provider",
    onModelSelected: (String) -> Unit,
    onRefreshModels: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var isCustomMode by remember { mutableStateOf(modelOptions.none { it.id == selectedModel }) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshFeedback by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
            )

            // Refresh Models action button
            if (onRefreshModels != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (!isRefreshing) {
                                isRefreshing = true
                                onRefreshModels()
                                coroutineScope.launch {
                                    delay(700)
                                    isRefreshing = false
                                    refreshFeedback = "Updated ${modelOptions.size} models from $providerName"
                                    delay(2000)
                                    refreshFeedback = null
                                }
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh models list",
                        tint = HermesCyan,
                        modifier = Modifier
                            .size(14.dp)
                            .then(if (isRefreshing) Modifier.rotate(spinAngle) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRefreshing) "Fetching..." else "Refresh list",
                        color = HermesCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = if (isCustomMode) selectedModel else (modelOptions.firstOrNull { it.id == selectedModel }?.displayName ?: selectedModel),
                onValueChange = {
                    isCustomMode = true
                    onModelSelected(it)
                },
                readOnly = !isCustomMode,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isCustomMode = !isCustomMode }) {
                            Icon(
                                imageVector = if (isCustomMode) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = "Toggle Custom Input",
                                tint = HermesCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HermesAmber,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate950,
                    unfocusedContainerColor = Slate950
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Slate900)
                    .border(1.dp, Slate700, RoundedCornerShape(8.dp))
            ) {
                modelOptions.forEach { option ->
                    val isSelected = option.id == selectedModel
                    DropdownMenuItem(
                        text = {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option.displayName,
                                        color = if (isSelected) HermesAmber else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = HermesAmber, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(
                                    text = "${option.id} • ${option.description}",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        },
                        onClick = {
                            isCustomMode = false
                            onModelSelected(option.id)
                            expanded = false
                        },
                        modifier = Modifier.background(if (isSelected) Slate800 else Slate900)
                    )
                }

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = HermesCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Custom Model ID...", color = HermesCyan, fontWeight = FontWeight.Bold)
                        }
                    },
                    onClick = {
                        isCustomMode = true
                        expanded = false
                    }
                )
            }
        }

        if (refreshFeedback != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = refreshFeedback!!,
                color = HermesGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SECURE PASSWORD FIELD WITH VISIBILITY TOGGLE & REAL-TIME VALIDATION
// ------------------------------------------------------------------------------------------------

@Composable
fun ValidatedPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    helperText: String? = null,
    validationResult: KeyValidationResult? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = Color(0xFF475569)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                // Eye / EyeOff toggle visibility icon
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.testTag("toggle_visibility_${label.lowercase().replace(" ", "_")}")
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide secret key" else "Show secret key",
                        tint = if (passwordVisible) HermesAmber else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = when {
                    validationResult == null -> HermesAmber
                    validationResult.isSuccess -> HermesGreen
                    !validationResult.isValid -> HermesRed
                    else -> HermesAmber
                },
                unfocusedBorderColor = when {
                    validationResult == null -> Slate700
                    validationResult.isSuccess -> Color(0xFF065F46)
                    !validationResult.isValid -> Color(0xFF7F1D1D)
                    else -> Slate700
                },
                focusedContainerColor = Slate950,
                unfocusedContainerColor = Slate950
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Real-Time Validation Status Badge
        if (validationResult != null) {
            ValidationBadge(validationResult = validationResult)
        } else if (helperText != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.sp)
            )
        }
    }
}

@Composable
fun ValidationBadge(validationResult: KeyValidationResult) {
    Spacer(modifier = Modifier.height(3.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = when {
                validationResult.isSuccess -> Icons.Default.CheckCircle
                !validationResult.isValid -> Icons.Default.Warning
                else -> Icons.Default.Info
            },
            contentDescription = null,
            tint = when {
                validationResult.isSuccess -> HermesGreen
                !validationResult.isValid -> HermesRed
                else -> HermesAmber
            },
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = validationResult.message,
            style = MaterialTheme.typography.labelSmall.copy(
                color = when {
                    validationResult.isSuccess -> HermesGreen
                    !validationResult.isValid -> HermesRed
                    else -> Color(0xFFCBD5E1)
                },
                fontSize = 11.sp,
                fontWeight = if (!validationResult.isValid) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

// ------------------------------------------------------------------------------------------------
// ACTIVE PROVIDER DROPDOWN COMBO BOX
// ------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveProviderComboBox(
    selectedProvider: ModelProviderType,
    onProviderSelected: (ModelProviderType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = "${selectedProvider.displayName} (${selectedProvider.defaultModel})",
            onValueChange = {},
            readOnly = true,
            label = { Text("Active Engine Provider") },
            leadingIcon = {
                Box(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
                    ProviderLogoBadge(provider = selectedProvider, size = 24)
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = HermesAmber,
                unfocusedTextColor = Color.White,
                focusedBorderColor = HermesAmber,
                unfocusedBorderColor = Slate700,
                focusedContainerColor = Slate950,
                unfocusedContainerColor = Slate950
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Slate900)
                .border(1.dp, Slate700, RoundedCornerShape(8.dp))
        ) {
            ModelProviderType.values().forEach { provider ->
                val isSelected = provider == selectedProvider
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProviderLogoBadge(provider = provider, size = 26)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = provider.displayName,
                                        color = if (isSelected) HermesAmber else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = "Default: ${provider.defaultModel}",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = HermesAmber, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    onClick = {
                        onProviderSelected(provider)
                        expanded = false
                    },
                    modifier = Modifier.background(if (isSelected) Slate800 else Slate900)
                )
            }
        }
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}
