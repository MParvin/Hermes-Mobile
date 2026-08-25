package com.example.engine.agent

import android.content.Context
import com.example.data.local.entities.MessageEntity
import com.example.data.model.AgentPersonality
import com.example.data.model.ApprovalStatus
import com.example.data.model.ModelProviderType
import com.example.data.model.RiskLevel
import com.example.data.repository.HermesRepository
import com.example.engine.model.ModelMessage
import com.example.engine.model.ModelResponse
import com.example.engine.model.ModelService
import com.example.engine.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

data class AgentExecutionStep(
    val replyText: String,
    val thoughts: String?,
    val toolCallName: String?,
    val toolCallParams: Map<String, Any?>?,
    val toolResultJson: String?,
    val requiresApproval: Boolean,
    val pendingApprovalId: String?
)

class AutonomousAgentEngine(
    private val context: Context,
    private val repository: HermesRepository,
    private val modelService: ModelService
) {

    private val toolRegistry = ToolRegistry.instance

    /**
     * Executes an agent turn with autonomous tool execution loop
     */
    suspend fun processUserTurn(
        conversationId: String,
        userPrompt: String,
        provider: ModelProviderType,
        personality: AgentPersonality,
        maxToolLoops: Int = 4
    ): List<AgentExecutionStep> = withContext(Dispatchers.IO) {
        val steps = mutableListOf<AgentExecutionStep>()

        // 1. Save user message
        val userMsgId = UUID.randomUUID().toString()
        repository.saveMessage(
            MessageEntity(
                id = userMsgId,
                conversationId = conversationId,
                sender = "user",
                content = userPrompt,
                timestamp = System.currentTimeMillis()
            )
        )

        val conversationHistory = mutableListOf<ModelMessage>()
        val memories = repository.getTopMemoriesForContext()
        val memoryContext = if (memories.isNotEmpty()) {
            "\nLONG-TERM EPISODIC & FACT MEMORY STORE:\n" + memories.joinToString("\n") { "• [${it.category}]: ${it.subject} -> ${it.content}" }
        } else ""

        val systemPrompt = "${personality.systemPrompt}$memoryContext"
        val toolsPrompt = toolRegistry.buildToolsSystemPrompt()

        conversationHistory.add(ModelMessage("user", userPrompt))

        var loopCount = 0
        var continueLoop = true

        while (continueLoop && loopCount < maxToolLoops) {
            loopCount++

            // Call Model
            val response: ModelResponse = modelService.generateCompletion(
                provider = provider,
                systemPrompt = systemPrompt,
                messages = conversationHistory,
                toolsPrompt = toolsPrompt
            )

            val rawText = response.content

            // Parse thoughts <thought>...</thought>
            val thoughtRegex = Regex("""<thought>(.*?)</thought>""", RegexOption.DOT_MATCHES_ALL)
            val thoughtMatch = thoughtRegex.find(rawText)
            val thoughts = thoughtMatch?.groupValues?.get(1)?.trim()
            val textWithoutThoughts = rawText.replace(thoughtRegex, "").trim()

            // Parse tool calls <tool_call>...</tool_call>
            val toolCallRegex = Regex("""<tool_call>(.*?)</tool_call>""", RegexOption.DOT_MATCHES_ALL)
            val toolMatch = toolCallRegex.find(textWithoutThoughts)

            if (toolMatch != null) {
                val toolCallStr = toolMatch.groupValues[1].trim()
                val (toolName, params) = parseToolCall(toolCallStr)

                val cleanReply = textWithoutThoughts.replace(toolCallRegex, "").trim()

                if (toolName != null) {
                    val tool = toolRegistry.getTool(toolName)
                    val isHighRisk = toolRegistry.requiresApproval(toolName, repository)

                    if (isHighRisk) {
                        // Needs explicit user approval before execution
                        val approvalId = UUID.randomUUID().toString()
                        val agentMsgId = UUID.randomUUID().toString()

                        repository.saveMessage(
                            MessageEntity(
                                id = agentMsgId,
                                conversationId = conversationId,
                                sender = "hermes",
                                content = cleanReply.ifBlank { "Requesting authorization to execute **$toolName**." },
                                thoughts = thoughts,
                                toolCallJson = JSONObject().apply {
                                    put("name", toolName)
                                    put("parameters", JSONObject(params))
                                    put("riskLevel", tool?.riskLevel?.name ?: "HIGH")
                                    put("approvalId", approvalId)
                                }.toString(),
                                modelBadge = response.modelBadge,
                                pendingApprovalId = approvalId,
                                timestamp = System.currentTimeMillis()
                            )
                        )

                        steps.add(
                            AgentExecutionStep(
                                replyText = cleanReply,
                                thoughts = thoughts,
                                toolCallName = toolName,
                                toolCallParams = params,
                                toolResultJson = null,
                                requiresApproval = true,
                                pendingApprovalId = approvalId
                            )
                        )

                        continueLoop = false // Pause loop for approval
                    } else {
                        // Auto-approved / low-medium risk -> execute immediately
                        val execResult = toolRegistry.executeTool(
                            toolName = toolName,
                            params = params,
                            context = context,
                            repository = repository,
                            triggeredBy = "In-App Chat Loop",
                            approvalStatus = ApprovalStatus.AUTO_APPROVED
                        )

                        val agentMsgId = UUID.randomUUID().toString()
                        repository.saveMessage(
                            MessageEntity(
                                id = agentMsgId,
                                conversationId = conversationId,
                                sender = "hermes",
                                content = cleanReply.ifBlank { execResult.summary },
                                thoughts = thoughts,
                                toolCallJson = JSONObject().apply {
                                    put("name", toolName)
                                    put("parameters", JSONObject(params))
                                }.toString(),
                                toolResultJson = execResult.resultJson,
                                modelBadge = response.modelBadge,
                                timestamp = System.currentTimeMillis()
                            )
                        )

                        steps.add(
                            AgentExecutionStep(
                                replyText = cleanReply,
                                thoughts = thoughts,
                                toolCallName = toolName,
                                toolCallParams = params,
                                toolResultJson = execResult.resultJson,
                                requiresApproval = false,
                                pendingApprovalId = null
                            )
                        )

                        // Feed tool result back to model for next synthesis step
                        conversationHistory.add(ModelMessage("assistant", rawText))
                        conversationHistory.add(
                            ModelMessage(
                                "user",
                                "Tool '$toolName' execution result:\n${execResult.resultJson}\nNow provide your final synthesis or next tool action."
                            )
                        )
                    }
                } else {
                    // Could not parse tool name
                    val agentMsgId = UUID.randomUUID().toString()
                    repository.saveMessage(
                        MessageEntity(
                            id = agentMsgId,
                            conversationId = conversationId,
                            sender = "hermes",
                            content = textWithoutThoughts,
                            thoughts = thoughts,
                            modelBadge = response.modelBadge,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    steps.add(
                        AgentExecutionStep(
                            replyText = textWithoutThoughts,
                            thoughts = thoughts,
                            toolCallName = null,
                            toolCallParams = null,
                            toolResultJson = null,
                            requiresApproval = false,
                            pendingApprovalId = null
                        )
                    )
                    continueLoop = false
                }
            } else {
                // Normal final response without tool call
                val agentMsgId = UUID.randomUUID().toString()
                repository.saveMessage(
                    MessageEntity(
                        id = agentMsgId,
                        conversationId = conversationId,
                        sender = "hermes",
                        content = textWithoutThoughts,
                        thoughts = thoughts,
                        modelBadge = response.modelBadge,
                        timestamp = System.currentTimeMillis()
                    )
                )

                steps.add(
                    AgentExecutionStep(
                        replyText = textWithoutThoughts,
                        thoughts = thoughts,
                        toolCallName = null,
                        toolCallParams = null,
                        toolResultJson = null,
                        requiresApproval = false,
                        pendingApprovalId = null
                    )
                )
                continueLoop = false
            }
        }

        steps
    }

    /**
     * Resumes an execution step after user approves high-risk action
     */
    suspend fun executeApprovedTool(
        conversationId: String,
        messageId: String,
        toolName: String,
        params: Map<String, Any?>
    ) = withContext(Dispatchers.IO) {
        val execResult = toolRegistry.executeTool(
            toolName = toolName,
            params = params,
            context = context,
            repository = repository,
            triggeredBy = "User Manual Approval",
            approvalStatus = ApprovalStatus.APPROVED
        )

        // Save tool result update
        val toolMsgId = UUID.randomUUID().toString()
        repository.saveMessage(
            MessageEntity(
                id = toolMsgId,
                conversationId = conversationId,
                sender = "tool",
                content = "✅ **Action Executed**: ${execResult.summary}",
                toolCallJson = JSONObject().apply {
                    put("name", toolName)
                    put("parameters", JSONObject(params))
                }.toString(),
                toolResultJson = execResult.resultJson,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun parseToolCall(jsonStr: String): Pair<String?, Map<String, Any?>> {
        return try {
            val json = JSONObject(jsonStr)
            val name = json.optString("name")
            val paramsObj = json.optJSONObject("parameters")
            val map = mutableMapOf<String, Any?>()
            if (paramsObj != null) {
                val keys = paramsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = paramsObj.get(k)
                }
            }
            Pair(name.ifBlank { null }, map)
        } catch (_: Exception) {
            Pair(null, emptyMap())
        }
    }
}
