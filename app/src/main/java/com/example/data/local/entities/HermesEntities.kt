package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.ApprovalStatus
import com.example.data.model.DeliverChannel
import com.example.data.model.MemoryCategory
import com.example.data.model.RiskLevel
import com.example.data.model.TaskStatus

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val personalityId: String = "hermes_core",
    val modelProvider: String = "GEMINI",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val sender: String, // "user", "hermes", "system", "tool"
    val content: String,
    val thoughts: String? = null,
    val toolCallJson: String? = null, // JSON representation of tool calls
    val toolResultJson: String? = null, // JSON representation of tool execution
    val modelBadge: String? = null,
    val pendingApprovalId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "fact_memories")
data class FactMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: MemoryCategory,
    val subject: String,
    val content: String,
    val confidence: Float = 1.0f,
    val source: String = "Conversation",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val name: String, // e.g. "morning_briefing"
    val displayName: String,
    val description: String,
    val triggerPattern: String,
    val instructions: String,
    val toolSequenceJson: String, // JSON list of tools to invoke
    val isAutoSaved: Boolean = false,
    val invocationCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scheduled_tasks")
data class ScheduledTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val naturalLanguagePrompt: String,
    val cronExpression: String, // e.g. "0 8 * * *" or "interval:60"
    val targetChannel: DeliverChannel = DeliverChannel.ALL,
    val isEnabled: Boolean = true,
    val lastRunTime: Long? = null,
    val nextRunTime: Long = System.currentTimeMillis() + 60_000L,
    val lastResultSummary: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tool_audit_logs")
data class ToolAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolName: String,
    val riskLevel: RiskLevel,
    val inputJson: String,
    val outputJson: String,
    val approvalStatus: ApprovalStatus,
    val executionDurationMs: Long,
    val error: String? = null,
    val triggeredBy: String = "Autonomous Loop", // "User Chat", "Telegram", "Cron", "Subagent", "Goal"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "subagent_tasks")
data class SubagentTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val objective: String,
    val assignedModel: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val progressPercent: Int = 0,
    val logs: String = "",
    val resultSummary: String? = null,
    val parentConversationId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "autonomous_goals")
data class AutonomousGoalEntity(
    @PrimaryKey val id: String,
    val objective: String,
    val successCriteria: String,
    val maxIterations: Int = 5,
    val currentIteration: Int = 0,
    val status: TaskStatus = TaskStatus.RUNNING,
    val evidenceLogs: String = "[]", // JSON array of verified tool evidence
    val judgeFeedback: String? = null,
    val isAchieved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "mcp_servers")
data class McpServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val endpointUrl: String,
    val isEnabled: Boolean = true,
    val availableToolsCount: Int = 0,
    val lastPingStatus: String = "ONLINE",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
