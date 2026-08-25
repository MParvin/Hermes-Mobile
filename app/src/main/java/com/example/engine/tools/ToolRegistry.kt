package com.example.engine.tools

import android.content.Context
import com.example.data.model.ApprovalStatus
import com.example.data.model.RiskLevel
import com.example.data.repository.HermesRepository
import org.json.JSONArray
import org.json.JSONObject

class ToolRegistry private constructor() {

    private val toolsMap = mutableMapOf<String, HermesTool>()

    init {
        registerTool(WebSearchTool())
        registerTool(ReadSmsTool())
        registerTool(SendSmsTool())
        registerTool(MakePhoneCallTool())
        registerTool(ReadCallLogTool())
        registerTool(ReadContactsTool())
        registerTool(CalendarTool())
        registerTool(DeviceLocationTool())
        registerTool(DeviceTelemetryTool())
        registerTool(InstalledAppsTool())
        registerTool(PostNotificationTool())
        registerTool(HttpRequestTool())
        registerTool(SaveLearnedFactTool())
        registerTool(QueryMemoryTool())
        registerTool(SaveSkillTool())
        registerTool(ScheduleAutomationTool())
        registerTool(SpawnSubagentTool())
        registerTool(ExecuteGoalLoopTool())
        registerTool(MathCalculatorTool())
    }

    fun registerTool(tool: HermesTool) {
        toolsMap[tool.name] = tool
    }

    fun getAllTools(): List<HermesTool> = toolsMap.values.toList()

    fun getTool(name: String): HermesTool? = toolsMap[name]

    /**
     * Builds standard system instructions declaring all available tools and invocation schemas
     */
    fun buildToolsSystemPrompt(): String {
        val sb = StringBuilder()
        sb.append("AVAILABLE TOOLS ON THIS ANDROID DEVICE:\n")
        sb.append("You can invoke tools by outputting a tool call formatted exactly like:\n")
        sb.append("<tool_call>{\"name\": \"tool_name\", \"parameters\": {\"param1\": \"value1\"}}</tool_call>\n\n")

        for (tool in toolsMap.values) {
            sb.append("• Tool: ${tool.name} [Risk: ${tool.riskLevel.displayName}]\n")
            sb.append("  Description: ${tool.description}\n")
            sb.append("  Category: ${tool.category}\n")
            sb.append("  Parameters: ${tool.parameterSchema}\n\n")
        }
        return sb.toString()
    }

    /**
     * Checks if a tool is considered high-risk and requires user confirmation
     */
    suspend fun requiresApproval(toolName: String, repository: HermesRepository): Boolean {
        val tool = toolsMap[toolName] ?: return false
        if (tool.riskLevel != RiskLevel.HIGH) return false

        // Check if user set auto-approve for this tool
        return !repository.isToolAutoApproved(toolName)
    }

    /**
     * Executes a tool, verifies kill-switch & enabled toggles, and records a tamper-evident audit log
     */
    suspend fun executeTool(
        toolName: String,
        params: Map<String, Any?>,
        context: Context,
        repository: HermesRepository,
        triggeredBy: String = "Autonomous Loop",
        approvalStatus: ApprovalStatus = ApprovalStatus.APPROVED
    ): ToolExecutionResult {
        val startTime = System.currentTimeMillis()

        // 1. Check Global Kill Switch
        if (repository.isKillSwitchActive()) {
            val res = ToolExecutionResult(
                isSuccess = false,
                resultJson = JSONObject().put("error", "GLOBAL_KILL_SWITCH_ACTIVE").toString(),
                summary = "Execution blocked by Global Kill Switch",
                error = "Global Kill Switch is active"
            )
            repository.logToolInvocation(
                toolName = toolName,
                riskLevel = RiskLevel.HIGH,
                inputJson = JSONObject(params).toString(),
                outputJson = res.resultJson,
                approvalStatus = ApprovalStatus.REJECTED,
                durationMs = System.currentTimeMillis() - startTime,
                error = res.error,
                triggeredBy = triggeredBy
            )
            return res
        }

        // 2. Check if Tool is Disabled in Settings
        if (!repository.isToolEnabled(toolName)) {
            val res = ToolExecutionResult(
                isSuccess = false,
                resultJson = JSONObject().put("error", "TOOL_DISABLED_BY_USER").toString(),
                summary = "Tool '$toolName' is disabled in Permissions & Access settings",
                error = "Tool disabled"
            )
            return res
        }

        val tool = toolsMap[toolName] ?: return ToolExecutionResult(
            isSuccess = false,
            resultJson = JSONObject().put("error", "TOOL_NOT_FOUND").toString(),
            summary = "Tool '$toolName' not registered in agent registry",
            error = "Unknown tool"
        )

        // 3. Execute Tool
        val result = try {
            tool.execute(context, params, repository)
        } catch (e: Exception) {
            ToolExecutionResult(
                isSuccess = false,
                resultJson = JSONObject().put("error", e.message).toString(),
                summary = "Exception during tool execution: ${e.message}",
                error = e.localizedMessage
            )
        }

        val duration = System.currentTimeMillis() - startTime

        // 4. Log to Room Audit Trail
        repository.logToolInvocation(
            toolName = toolName,
            riskLevel = tool.riskLevel,
            inputJson = JSONObject(params).toString(),
            outputJson = result.resultJson,
            approvalStatus = approvalStatus,
            durationMs = duration,
            error = result.error,
            triggeredBy = triggeredBy
        )

        return result
    }

    companion object {
        val instance: ToolRegistry by lazy { ToolRegistry() }
    }
}
