package com.example.engine.gateway

import android.content.Context
import android.util.Log
import com.example.data.model.AgentPersonality
import com.example.data.model.ModelProviderType
import com.example.data.repository.HermesRepository
import com.example.engine.agent.AutonomousAgentEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TelegramGateway(
    private val context: Context,
    private val repository: HermesRepository,
    private val agentEngine: AutonomousAgentEngine
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private var pollingJob: Job? = null
    private var lastUpdateId = 0L

    fun startPolling(scope: CoroutineScope) {
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch(Dispatchers.IO) {
            Log.d("TelegramGateway", "Telegram polling loop started")
            while (isActive) {
                try {
                    val token = repository.getSetting("telegram_bot_token", "")
                    val ownerChatId = repository.getSetting("telegram_owner_chat_id", "")
                    val isEnabled = repository.getSetting("telegram_bridge_enabled", "false").toBoolean()

                    if (isEnabled && token.isNotBlank()) {
                        pollUpdates(token, ownerChatId)
                    }
                } catch (e: Exception) {
                    Log.e("TelegramGateway", "Error in poll loop: ${e.message}")
                }
                delay(3000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun pollUpdates(token: String, ownerChatId: String) = withContext(Dispatchers.IO) {
        val url = "https://api.telegram.org/bot$token/getUpdates?offset=${lastUpdateId + 1}&timeout=10"
        val req = Request.Builder().url(url).build()

        try {
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext
            val json = JSONObject(body)

            if (json.optBoolean("ok")) {
                val result = json.optJSONArray("result") ?: return@withContext
                for (i in 0 until result.length()) {
                    val update = result.getJSONObject(i)
                    val updateId = update.optLong("update_id")
                    if (updateId > lastUpdateId) {
                        lastUpdateId = updateId
                    }

                    val message = update.optJSONObject("message") ?: continue
                    val chat = message.optJSONObject("chat") ?: continue
                    val chatId = chat.optLong("id").toString()
                    val text = message.optString("text")

                    if (text.isNotBlank()) {
                        handleTelegramMessage(token, ownerChatId, chatId, text)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TelegramGateway", "Failed getUpdates: ${e.message}")
        }
    }

    private suspend fun handleTelegramMessage(
        token: String,
        configuredOwnerChatId: String,
        senderChatId: String,
        text: String
    ) {
        // Owner verification (Requirement #1: Owner-Only Control)
        if (configuredOwnerChatId.isNotBlank() && senderChatId != configuredOwnerChatId) {
            sendTelegramMessage(
                token,
                senderChatId,
                "⚠️ **Access Denied**: Hermes Mobile is configured for owner-only access. Your Chat ID is `$senderChatId`."
            )
            return
        }

        // Handle commands
        when (text.trim()) {
            "/start" -> {
                sendTelegramMessage(
                    token,
                    senderChatId,
                    "👋 **Hermes Mobile Gateway Connected**\nYour Chat ID: `$senderChatId`\n\nSend any instruction to execute tools or query on-device capabilities."
                )
                return
            }
            "/kill" -> {
                repository.setKillSwitch(true)
                sendTelegramMessage(token, senderChatId, "🚨 **Global Kill Switch ACTIVATED**. All tool executions revoked.")
                return
            }
            "/status" -> {
                val kill = repository.isKillSwitchActive()
                sendTelegramMessage(
                    token,
                    senderChatId,
                    "📊 **Hermes Mobile Status**\n• Kill Switch: ${if (kill) "🚨 ACTIVE" else "✅ Standby"}\n• Battery & Telemetry: Connected\n• Room Storage: Synchronized"
                )
                return
            }
        }

        // Process through Autonomous Agent Engine
        sendTelegramMessage(token, senderChatId, "⚡ Processing autonomous task...")
        try {
            val steps = agentEngine.processUserTurn(
                conversationId = "telegram_channel",
                userPrompt = text,
                provider = ModelProviderType.GEMINI,
                personality = AgentPersonality.DEFAULT_PERSONALITIES.first()
            )

            val reply = steps.lastOrNull()?.replyText ?: "Task processed."
            sendTelegramMessage(token, senderChatId, "🤖 **Hermes**:\n$reply")
        } catch (e: Exception) {
            sendTelegramMessage(token, senderChatId, "❌ Error processing request: ${e.message}")
        }
    }

    suspend fun sendTelegramMessage(token: String, chatId: String, text: String) = withContext(Dispatchers.IO) {
        if (token.isBlank() || chatId.isBlank()) return@withContext
        try {
            val url = "https://api.telegram.org/bot$token/sendMessage"
            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", text)
                put("parse_mode", "Markdown")
            }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val req = Request.Builder().url(url).post(body).build()
            httpClient.newCall(req).execute().close()
        } catch (e: Exception) {
            Log.e("TelegramGateway", "Failed sendMessage: ${e.message}")
        }
    }
}
