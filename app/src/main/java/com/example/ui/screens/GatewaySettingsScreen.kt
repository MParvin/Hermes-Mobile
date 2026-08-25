package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.HermesAmber
import com.example.ui.theme.HermesCyan
import com.example.ui.theme.HermesGreen
import com.example.ui.theme.HermesRed
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.HermesViewModel

@Composable
fun GatewaySettingsScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    val isKillSwitchActive by viewModel.isKillSwitchActive.collectAsState()

    var geminiKey by remember { mutableStateOf(BuildConfig.GEMINI_API_KEY) }
    var anthropicKey by remember { mutableStateOf("") }
    var openaiKey by remember { mutableStateOf("") }
    var openRouterKey by remember { mutableStateOf("") }
    var localEndpoint by remember { mutableStateOf("http://10.0.2.2:11434/v1/chat/completions") }

    var telegramToken by remember { mutableStateOf("") }
    var telegramOwnerId by remember { mutableStateOf("") }
    var isTelegramEnabled by remember { mutableStateOf(false) }

    var httpPort by remember { mutableStateOf("8080") }
    var isHttpEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Model API Credentials
        Text(
            text = "MODEL PROVIDER KEYS & CONFIGURATION",
            style = MaterialTheme.typography.labelSmall.copy(color = HermesAmber, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsCard {
            OutlinedTextField(
                value = geminiKey,
                onValueChange = {
                    geminiKey = it
                    viewModel.saveSetting("gemini_api_key", it)
                },
                label = { Text("Google Gemini API Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = anthropicKey,
                onValueChange = {
                    anthropicKey = it
                    viewModel.saveSetting("anthropic_api_key", it)
                },
                label = { Text("Anthropic Claude API Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = openaiKey,
                onValueChange = {
                    openaiKey = it
                    viewModel.saveSetting("openai_api_key", it)
                },
                label = { Text("OpenAI API Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = openRouterKey,
                onValueChange = {
                    openRouterKey = it
                    viewModel.saveSetting("openrouter_api_key", it)
                },
                label = { Text("OpenRouter API Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = localEndpoint,
                onValueChange = {
                    localEndpoint = it
                    viewModel.saveSetting("local_endpoint_url", it)
                },
                label = { Text("Local Ollama / LM Studio URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Telegram Gateway
        Text(
            text = "TELEGRAM BOT GATEWAY BRIDGE",
            style = MaterialTheme.typography.labelSmall.copy(color = HermesCyan, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Telegram Bridge", style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    Text("Long-polling worker for owner-only chat", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)))
                }
                Switch(
                    checked = isTelegramEnabled,
                    onCheckedChange = {
                        isTelegramEnabled = it
                        viewModel.saveSetting("telegram_bridge_enabled", it.toString())
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = HermesCyan)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = telegramToken,
                onValueChange = {
                    telegramToken = it
                    viewModel.saveSetting("telegram_bot_token", it)
                },
                label = { Text("Telegram Bot Token (from @BotFather)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = telegramOwnerId,
                onValueChange = {
                    telegramOwnerId = it
                    viewModel.saveSetting("telegram_owner_chat_id", it)
                },
                label = { Text("Owner Telegram Chat ID (Enforces Owner-Only Control)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Local HTTP API Server
        Text(
            text = "EMBEDDED LOCAL HTTP API GATEWAY",
            style = MaterialTheme.typography.labelSmall.copy(color = HermesGreen, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Local HTTP Daemon", style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    Text("OpenAI-compatible /v1/chat/completions endpoint", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)))
                }
                Switch(
                    checked = isHttpEnabled,
                    onCheckedChange = {
                        isHttpEnabled = it
                        viewModel.saveSetting("local_http_enabled", it.toString())
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = HermesGreen)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = httpPort,
                onValueChange = {
                    httpPort = it
                    viewModel.saveSetting("local_http_port", it)
                },
                label = { Text("Server Port (default: 8080)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

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

        Spacer(modifier = Modifier.height(30.dp))
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
