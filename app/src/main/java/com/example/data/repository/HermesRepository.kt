package com.example.data.repository

import android.content.Context
import com.example.data.local.HermesDatabase
import com.example.data.local.entities.AppSettingEntity
import com.example.data.local.entities.AutonomousGoalEntity
import com.example.data.local.entities.ConversationEntity
import com.example.data.local.entities.FactMemoryEntity
import com.example.data.local.entities.McpServerEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.ScheduledTaskEntity
import com.example.data.local.entities.SkillEntity
import com.example.data.local.entities.SubagentTaskEntity
import com.example.data.local.entities.ToolAuditLogEntity
import com.example.data.model.ApprovalStatus
import com.example.data.model.DeliverChannel
import com.example.data.model.MemoryCategory
import com.example.data.model.ModelProviderType
import com.example.data.model.RiskLevel
import com.example.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class HermesRepository(private val database: HermesDatabase) {

    // Conversations & Messages
    fun getAllConversations(): Flow<List<ConversationEntity>> =
        database.conversationDao().getAllConversations()

    suspend fun getConversation(id: String): ConversationEntity? =
        database.conversationDao().getConversationById(id)

    suspend fun saveConversation(conversation: ConversationEntity) =
        database.conversationDao().insertOrUpdate(conversation)

    suspend fun deleteConversation(id: String) =
        database.conversationDao().deleteById(id)

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> =
        database.messageDao().getMessagesForConversation(conversationId)

    suspend fun saveMessage(message: MessageEntity) =
        database.messageDao().insertMessage(message)

    suspend fun updateMessage(message: MessageEntity) =
        database.messageDao().updateMessage(message)

    suspend fun clearConversation(conversationId: String) =
        database.messageDao().clearConversationMessages(conversationId)

    // Fact Memories
    fun getAllMemories(): Flow<List<FactMemoryEntity>> =
        database.factMemoryDao().getAllMemories()

    fun searchMemories(query: String): Flow<List<FactMemoryEntity>> =
        database.factMemoryDao().searchMemories(query)

    suspend fun getTopMemoriesForContext(): List<FactMemoryEntity> =
        database.factMemoryDao().getTopMemoriesForContext()

    suspend fun saveMemory(
        category: MemoryCategory,
        subject: String,
        content: String,
        confidence: Float = 1.0f,
        source: String = "Conversation"
    ): Long {
        val entity = FactMemoryEntity(
            category = category,
            subject = subject,
            content = content,
            confidence = confidence,
            source = source
        )
        return database.factMemoryDao().insertMemory(entity)
    }

    suspend fun updateMemory(memory: FactMemoryEntity) =
        database.factMemoryDao().updateMemory(memory)

    suspend fun deleteMemory(id: Long) =
        database.factMemoryDao().deleteMemoryById(id)

    // Skills
    fun getAllSkills(): Flow<List<SkillEntity>> =
        database.skillDao().getAllSkills()

    suspend fun getSkill(name: String): SkillEntity? =
        database.skillDao().getSkillByName(name)

    suspend fun saveSkill(skill: SkillEntity) =
        database.skillDao().insertSkill(skill)

    suspend fun incrementSkillUse(name: String) =
        database.skillDao().incrementInvocationCount(name)

    suspend fun deleteSkill(name: String) =
        database.skillDao().deleteSkill(name)

    // Scheduled Automations
    fun getAllScheduledTasks(): Flow<List<ScheduledTaskEntity>> =
        database.scheduledTaskDao().getAllTasks()

    suspend fun saveScheduledTask(task: ScheduledTaskEntity) =
        database.scheduledTaskDao().insertTask(task)

    suspend fun updateScheduledTask(task: ScheduledTaskEntity) =
        database.scheduledTaskDao().updateTask(task)

    suspend fun deleteScheduledTask(id: String) =
        database.scheduledTaskDao().deleteTask(id)

    suspend fun getDueTasks(currentTime: Long): List<ScheduledTaskEntity> =
        database.scheduledTaskDao().getDueTasks(currentTime)

    // Tool Audit Logs
    fun getAllAuditLogs(): Flow<List<ToolAuditLogEntity>> =
        database.toolAuditLogDao().getAllAuditLogs()

    fun getRecentAuditLogs(): Flow<List<ToolAuditLogEntity>> =
        database.toolAuditLogDao().getRecentAuditLogs()

    suspend fun logToolInvocation(
        toolName: String,
        riskLevel: RiskLevel,
        inputJson: String,
        outputJson: String,
        approvalStatus: ApprovalStatus,
        durationMs: Long,
        error: String? = null,
        triggeredBy: String = "Autonomous Loop"
    ): Long {
        val entity = ToolAuditLogEntity(
            toolName = toolName,
            riskLevel = riskLevel,
            inputJson = inputJson,
            outputJson = outputJson,
            approvalStatus = approvalStatus,
            executionDurationMs = durationMs,
            error = error,
            triggeredBy = triggeredBy
        )
        return database.toolAuditLogDao().insertAuditLog(entity)
    }

    suspend fun clearAuditLogs() =
        database.toolAuditLogDao().clearAllAuditLogs()

    // Subagent Tasks
    fun getAllSubagents(): Flow<List<SubagentTaskEntity>> =
        database.subagentTaskDao().getAllSubagents()

    suspend fun saveSubagent(task: SubagentTaskEntity) =
        database.subagentTaskDao().insertSubagent(task)

    suspend fun updateSubagent(task: SubagentTaskEntity) =
        database.subagentTaskDao().updateSubagent(task)

    suspend fun deleteSubagent(id: String) =
        database.subagentTaskDao().deleteSubagent(id)

    // Autonomous Goals
    fun getAllGoals(): Flow<List<AutonomousGoalEntity>> =
        database.autonomousGoalDao().getAllGoals()

    suspend fun getGoal(id: String): AutonomousGoalEntity? =
        database.autonomousGoalDao().getGoalById(id)

    suspend fun saveGoal(goal: AutonomousGoalEntity) =
        database.autonomousGoalDao().insertGoal(goal)

    suspend fun updateGoal(goal: AutonomousGoalEntity) =
        database.autonomousGoalDao().updateGoal(goal)

    suspend fun deleteGoal(id: String) =
        database.autonomousGoalDao().deleteGoal(id)

    // MCP Servers
    fun getAllMcpServers(): Flow<List<McpServerEntity>> =
        database.mcpServerDao().getAllMcpServers()

    suspend fun saveMcpServer(server: McpServerEntity) =
        database.mcpServerDao().insertMcpServer(server)

    suspend fun deleteMcpServer(id: String) =
        database.mcpServerDao().deleteMcpServer(id)

    // App Settings & Keys
    suspend fun getSetting(key: String, defaultValue: String = ""): String {
        return database.appSettingDao().getSettingValue(key) ?: defaultValue
    }

    suspend fun setSetting(key: String, value: String) {
        database.appSettingDao().setSetting(AppSettingEntity(key, value))
    }

    fun getAllSettings(): Flow<List<AppSettingEntity>> =
        database.appSettingDao().getAllSettings()

    suspend fun isKillSwitchActive(): Boolean {
        return getSetting("kill_switch_active", "false").toBoolean()
    }

    suspend fun setKillSwitch(active: Boolean) {
        setSetting("kill_switch_active", active.toString())
    }

    suspend fun isToolAutoApproved(toolName: String): Boolean {
        return getSetting("auto_approve_$toolName", "false").toBoolean()
    }

    suspend fun setToolAutoApprove(toolName: String, autoApprove: Boolean) {
        setSetting("auto_approve_$toolName", autoApprove.toString())
    }

    suspend fun isToolEnabled(toolName: String): Boolean {
        return getSetting("tool_enabled_$toolName", "true").toBoolean()
    }

    suspend fun setToolEnabled(toolName: String, enabled: Boolean) {
        setSetting("tool_enabled_$toolName", enabled.toString())
    }

    companion object {
        @Volatile
        private var INSTANCE: HermesRepository? = null

        fun getInstance(context: Context): HermesRepository {
            return INSTANCE ?: synchronized(this) {
                val db = HermesDatabase.getInstance(context)
                val repo = HermesRepository(db)
                INSTANCE = repo
                repo
            }
        }
    }
}
