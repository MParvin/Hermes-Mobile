package com.example.engine.gateway

import android.util.Log
import com.example.data.model.AgentPersonality
import com.example.data.model.ModelProviderType
import com.example.data.repository.HermesRepository
import com.example.engine.agent.AutonomousAgentEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class LocalHttpServer(
    private val repository: HermesRepository,
    private val agentEngine: AutonomousAgentEngine
) {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    var isRunning = false
        private set

    fun start(scope: CoroutineScope, port: Int = 8080) {
        if (serverJob?.isActive == true) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                Log.d("LocalHttpServer", "Local HTTP Gateway listening on port $port")

                while (isActive && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        launch(Dispatchers.IO) {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalHttpServer", "Server failed to start on port $port: ${e.message}")
            } finally {
                isRunning = false
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {}
        serverJob?.cancel()
        serverJob = null
        isRunning = false
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val writer = PrintWriter(s.getOutputStream())

            val requestLine = reader.readLine() ?: return@use
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@use

            val method = parts[0]
            val path = parts[1]

            // Read headers
            var line: String?
            var contentLength = 0
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                if (line!!.lowercase().startsWith("content-length:")) {
                    contentLength = line!!.substring(15).trim().toIntOrNull() ?: 0
                }
            }

            // Read body if POST
            val bodyBuilder = StringBuilder()
            if (contentLength > 0) {
                val buffer = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(buffer, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                bodyBuilder.append(buffer, 0, read)
            }

            val body = bodyBuilder.toString()

            // Router
            when {
                path == "/v1/status" || path == "/api/status" -> {
                    val statusJson = JSONObject().apply {
                        put("service", "Hermes Mobile Agent")
                        put("version", "1.0.0")
                        put("status", "ONLINE")
                        put("killSwitchActive", repository.isKillSwitchActive())
                    }.toString()
                    sendJsonResponse(writer, 200, statusJson)
                }
                path == "/v1/chat/completions" || path == "/api/agent" -> {
                    try {
                        val inputJson = JSONObject(body.ifBlank { "{}" })
                        val prompt = inputJson.optString("prompt").ifBlank {
                            inputJson.optJSONArray("messages")?.let { arr ->
                                arr.optJSONObject(arr.length() - 1)?.optString("content")
                            } ?: "status"
                        }

                        val steps = agentEngine.processUserTurn(
                            conversationId = "local_http_gateway",
                            userPrompt = prompt,
                            provider = ModelProviderType.GEMINI,
                            personality = AgentPersonality.DEFAULT_PERSONALITIES.first()
                        )

                        val reply = steps.lastOrNull()?.replyText ?: "Processed"
                        val respJson = JSONObject().apply {
                            put("id", "chatcmpl-${System.currentTimeMillis()}")
                            put("object", "chat.completion")
                            put("created", System.currentTimeMillis() / 1000)
                            put("choices", org.json.JSONArray().put(JSONObject().apply {
                                put("index", 0)
                                put("message", JSONObject().apply {
                                    put("role", "assistant")
                                    put("content", reply)
                                })
                                put("finish_reason", "stop")
                            }))
                        }.toString()
                        sendJsonResponse(writer, 200, respJson)
                    } catch (e: Exception) {
                        sendJsonResponse(writer, 400, JSONObject().put("error", e.message).toString())
                    }
                }
                else -> {
                    sendJsonResponse(writer, 404, JSONObject().put("error", "Endpoint not found: $path").toString())
                }
            }
        }
    }

    private fun sendJsonResponse(writer: PrintWriter, statusCode: Int, json: String) {
        val statusText = if (statusCode == 200) "OK" else if (statusCode == 404) "Not Found" else "Bad Request"
        writer.print("HTTP/1.1 $statusCode $statusText\r\n")
        writer.print("Content-Type: application/json; charset=utf-8\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Content-Length: ${json.toByteArray().size}\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.print(json)
        writer.flush()
    }
}
