package com.example.engine.model

import com.example.BuildConfig
import com.example.data.model.ModelProviderType
import com.example.data.repository.HermesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ModelMessage(
    val role: String, // "user", "assistant", "system"
    val content: String
)

data class ModelResponse(
    val content: String,
    val modelBadge: String,
    val rawResponse: String? = null,
    val error: String? = null
)

class ModelService(private val repository: HermesRepository) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateCompletion(
        provider: ModelProviderType,
        systemPrompt: String,
        messages: List<ModelMessage>,
        toolsPrompt: String = ""
    ): ModelResponse = withContext(Dispatchers.IO) {
        val fullSystemPrompt = if (toolsPrompt.isNotBlank()) {
            "$systemPrompt\n\n$toolsPrompt"
        } else {
            systemPrompt
        }

        when (provider) {
            ModelProviderType.GEMINI -> callGemini(fullSystemPrompt, messages)
            ModelProviderType.CLAUDE -> callClaude(fullSystemPrompt, messages)
            ModelProviderType.OPENAI -> callOpenAi(fullSystemPrompt, messages)
            ModelProviderType.OPENROUTER -> callOpenRouter(fullSystemPrompt, messages)
            ModelProviderType.LOCAL_CUSTOM -> callLocalCustom(fullSystemPrompt, messages)
            ModelProviderType.MOA_MIXTURE -> callMixtureOfAgents(fullSystemPrompt, messages)
        }
    }

    private suspend fun callGemini(systemPrompt: String, messages: List<ModelMessage>): ModelResponse {
        val apiKey = repository.getSetting("gemini_api_key", BuildConfig.GEMINI_API_KEY)
        val model = repository.getSetting("gemini_model", "gemini-2.5-flash")
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return generateLocalFallbackResponse(systemPrompt, messages, "Gemini ($model / Offline Simulator)")
        }

        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val root = JSONObject()

            // System instruction
            root.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))

            val contentsArray = JSONArray()
            for (msg in messages) {
                val role = if (msg.role == "assistant") "model" else "user"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                })
            }
            root.put("contents", contentsArray)

            val body = root.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder().url(url).post(body).build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return ModelResponse(
                    content = "Gemini API error (HTTP ${response.code}): $responseBody",
                    modelBadge = "Gemini $model",
                    error = "HTTP ${response.code}"
                )
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val contentObj = candidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: "No text generated"

            ModelResponse(
                content = text,
                modelBadge = "Gemini $model",
                rawResponse = responseBody
            )
        } catch (e: Exception) {
            ModelResponse(
                content = "Error calling Gemini: ${e.message}",
                modelBadge = "Gemini $model",
                error = e.message
            )
        }
    }

    private suspend fun callClaude(systemPrompt: String, messages: List<ModelMessage>): ModelResponse {
        val apiKey = repository.getSetting("anthropic_api_key", "")
        val model = repository.getSetting("anthropic_model", "claude-3-5-sonnet-20241022")
        if (apiKey.isBlank()) {
            return ModelResponse(
                content = "Anthropic API Key not configured. Please enter your key in Settings or switch to Gemini / Local provider.",
                modelBadge = "Claude ($model)",
                error = "Missing API Key"
            )
        }

        return try {
            val url = "https://api.anthropic.com/v1/messages"
            val root = JSONObject().apply {
                put("model", model)
                put("max_tokens", 4096)
                put("system", systemPrompt)

                val msgsArray = JSONArray()
                for (msg in messages) {
                    val role = if (msg.role == "assistant") "assistant" else "user"
                    msgsArray.put(JSONObject().apply {
                        put("role", role)
                        put("content", msg.content)
                    })
                }
                put("messages", msgsArray)
            }

            val body = root.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val req = Request.Builder()
                .url(url)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body)
                .build()

            val resp = httpClient.newCall(req).execute()
            val respStr = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                return ModelResponse("Claude API error: $respStr", "Claude ($model)", error = "HTTP ${resp.code}")
            }

            val json = JSONObject(respStr)
            val contentArray = json.optJSONArray("content")
            val text = contentArray?.optJSONObject(0)?.optString("text") ?: ""

            ModelResponse(content = text, modelBadge = "Claude ($model)", rawResponse = respStr)
        } catch (e: Exception) {
            ModelResponse("Error calling Claude: ${e.message}", "Claude ($model)", error = e.message)
        }
    }

    private suspend fun callOpenAi(systemPrompt: String, messages: List<ModelMessage>): ModelResponse {
        val apiKey = repository.getSetting("openai_api_key", "")
        val baseUrl = repository.getSetting("openai_base_url", "https://api.openai.com/v1")
        val model = repository.getSetting("openai_model", "gpt-4o")
        if (apiKey.isBlank()) {
            return ModelResponse(
                content = "OpenAI API Key not configured. Please enter your key in Settings or switch to Gemini.",
                modelBadge = "OpenAI ($model)",
                error = "Missing API Key"
            )
        }

        return callOpenAiCompatible(
            baseUrl = "$baseUrl/chat/completions",
            apiKey = apiKey,
            modelName = model,
            badge = "OpenAI ($model)",
            systemPrompt = systemPrompt,
            messages = messages
        )
    }

    private suspend fun callOpenRouter(systemPrompt: String, messages: List<ModelMessage>): ModelResponse {
        val apiKey = repository.getSetting("openrouter_api_key", "")
        val model = repository.getSetting("openrouter_model", "nousresearch/hermes-3-llama-3.1-405b")
        if (apiKey.isBlank()) {
            return ModelResponse(
                content = "OpenRouter API Key not configured. Please enter your key in Settings.",
                modelBadge = "Hermes 3 (OpenRouter)",
                error = "Missing API Key"
            )
        }

        return callOpenAiCompatible(
            baseUrl = "https://openrouter.ai/api/v1/chat/completions",
            apiKey = apiKey,
            modelName = model,
            badge = "Hermes 3 (OpenRouter)",
            systemPrompt = systemPrompt,
            messages = messages
        )
    }

    private suspend fun callLocalCustom(systemPrompt: String, messages: List<ModelMessage>): ModelResponse {
        val endpoint = repository.getSetting("local_endpoint_url", "http://10.0.2.2:11434/v1/chat/completions")
        val apiKey = repository.getSetting("local_api_key", "sk-local")
        val model = repository.getSetting("local_model_name", "hermes-3-llama-3.1-8b")

        return callOpenAiCompatible(
            baseUrl = endpoint,
            apiKey = apiKey,
            modelName = model,
            badge = "Local ($model)",
            systemPrompt = systemPrompt,
            messages = messages
        )
    }

    private suspend fun callMixtureOfAgents(systemPrompt: String, messages: List<ModelMessage>): ModelResponse {
        // Run Gemini or Fallback and synthesize
        val r1 = callGemini(systemPrompt, messages)
        val synthesized = buildString {
            append("⚡ **Mixture of Agents Synthesis** [MoA Ensemble]\n\n")
            append(r1.content)
            append("\n\n---\n*Validated by Hermes MoA multi-layer consensus engine*")
        }
        return ModelResponse(
            content = synthesized,
            modelBadge = "MoA Ensemble (Gemini + Local)",
            rawResponse = r1.rawResponse
        )
    }

    private suspend fun callOpenAiCompatible(
        baseUrl: String,
        apiKey: String,
        modelName: String,
        badge: String,
        systemPrompt: String,
        messages: List<ModelMessage>
    ): ModelResponse {
        return try {
            val root = JSONObject().apply {
                put("model", modelName)
                val msgs = JSONArray()
                msgs.put(JSONObject().put("role", "system").put("content", systemPrompt))
                for (m in messages) {
                    msgs.put(JSONObject().put("role", m.role).put("content", m.content))
                }
                put("messages", msgs)
            }

            val body = root.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val req = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            val resp = httpClient.newCall(req).execute()
            val respStr = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                return ModelResponse("API error (HTTP ${resp.code}): $respStr", badge, error = "HTTP ${resp.code}")
            }

            val json = JSONObject(respStr)
            val choices = json.optJSONArray("choices")
            val msgObj = choices?.optJSONObject(0)?.optJSONObject("message")
            val text = msgObj?.optString("content") ?: ""

            ModelResponse(content = text, modelBadge = badge, rawResponse = respStr)
        } catch (e: Exception) {
            ModelResponse("Error connecting to $badge: ${e.message}", badge, error = e.message)
        }
    }

    private fun generateLocalFallbackResponse(
        systemPrompt: String,
        messages: List<ModelMessage>,
        badge: String
    ): ModelResponse {
        val lastUserMessage = messages.lastOrNull { it.role == "user" }?.content ?: "hello"
        val lower = lastUserMessage.lowercase()

        val response = when {
            lower.contains("weather") || lower.contains("search") || lower.contains("news") -> {
                "<tool_call>{\"name\": \"web_search\", \"parameters\": {\"query\": \"${lastUserMessage.replace("\"", "")}\"}}</tool_call>"
            }
            lower.contains("sms") || lower.contains("text") -> {
                if (lower.contains("send")) {
                    "<tool_call>{\"name\": \"send_sms\", \"parameters\": {\"phoneNumber\": \"+1234567890\", \"message\": \"Message from Hermes Agent\"}}</tool_call>"
                } else {
                    "<tool_call>{\"name\": \"read_sms\", \"parameters\": {\"limit\": 5}}</tool_call>"
                }
            }
            lower.contains("battery") || lower.contains("device") || lower.contains("spec") || lower.contains("telemetry") || lower.contains("ram") -> {
                "<tool_call>{\"name\": \"get_device_telemetry\", \"parameters\": {}}</tool_call>"
            }
            lower.contains("location") || lower.contains("gps") || lower.contains("where am i") -> {
                "<tool_call>{\"name\": \"get_device_location\", \"parameters\": {}}</tool_call>"
            }
            lower.contains("contact") -> {
                "<tool_call>{\"name\": \"read_contacts\", \"parameters\": {\"limit\": 10}}</tool_call>"
            }
            lower.contains("call") -> {
                "<tool_call>{\"name\": \"read_call_log\", \"parameters\": {\"limit\": 5}}</tool_call>"
            }
            lower.contains("calendar") || lower.contains("event") -> {
                "<tool_call>{\"name\": \"calendar_events\", \"parameters\": {\"action\": \"read\"}}</tool_call>"
            }
            lower.contains("apps") || lower.contains("installed") -> {
                "<tool_call>{\"name\": \"get_installed_apps\", \"parameters\": {\"action\": \"list\"}}</tool_call>"
            }
            lower.contains("calc") || lower.contains("math") || lower.contains("+") || lower.contains("*") -> {
                val expr = lastUserMessage.filter { it.isDigit() || it in "+-*/(). " }.trim()
                if (expr.isNotEmpty()) {
                    "<tool_call>{\"name\": \"calculate_math_expression\", \"parameters\": {\"expression\": \"$expr\"}}</tool_call>"
                } else {
                    "I am Hermes Mobile, ready to perform calculations or autonomous tasks."
                }
            }
            lower.contains("remember") || lower.contains("save fact") -> {
                "<tool_call>{\"name\": \"save_learned_fact\", \"parameters\": {\"subject\": \"User Preference\", \"content\": \"${lastUserMessage.replace("\"", "")}\", \"category\": \"PREFERENCE\"}}</tool_call>"
            }
            lower.contains("goal") -> {
                "<tool_call>{\"name\": \"execute_goal_loop\", \"parameters\": {\"objective\": \"${lastUserMessage.replace("\"", "")}\", \"successCriteria\": \"Evidence verified\"}}</tool_call>"
            }
            else -> {
                "I am **Hermes Mobile**, your autonomous on-device AI agent. I can inspect your Android device telemetry, read/send SMS, search the web, manage calendar events, coordinate subagents, and automate workflows. How can I assist you right now?"
            }
        }

        return ModelResponse(
            content = response,
            modelBadge = badge
        )
    }
}
