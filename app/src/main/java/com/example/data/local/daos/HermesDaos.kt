package com.example.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearConversationMessages(conversationId: String)
}

@Dao
interface FactMemoryDao {
    @Query("SELECT * FROM fact_memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<FactMemoryEntity>>

    @Query("SELECT * FROM fact_memories WHERE subject LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchMemories(query: String): Flow<List<FactMemoryEntity>>

    @Query("SELECT * FROM fact_memories ORDER BY confidence DESC LIMIT 30")
    suspend fun getTopMemoriesForContext(): List<FactMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: FactMemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: FactMemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: FactMemoryEntity)

    @Query("DELETE FROM fact_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)
}

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY invocationCount DESC, createdAt DESC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE name = :name")
    suspend fun getSkillByName(name: String): SkillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity)

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Query("UPDATE skills SET invocationCount = invocationCount + 1 WHERE name = :name")
    suspend fun incrementInvocationCount(name: String)

    @Query("DELETE FROM skills WHERE name = :name")
    suspend fun deleteSkill(name: String)
}

@Dao
interface ScheduledTaskDao {
    @Query("SELECT * FROM scheduled_tasks ORDER BY nextRunTime ASC")
    fun getAllTasks(): Flow<List<ScheduledTaskEntity>>

    @Query("SELECT * FROM scheduled_tasks WHERE isEnabled = 1 AND nextRunTime <= :currentTime")
    suspend fun getDueTasks(currentTime: Long): List<ScheduledTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduledTaskEntity)

    @Update
    suspend fun updateTask(task: ScheduledTaskEntity)

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)
}

@Dao
interface ToolAuditLogDao {
    @Query("SELECT * FROM tool_audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<ToolAuditLogEntity>>

    @Query("SELECT * FROM tool_audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAuditLogs(): Flow<List<ToolAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: ToolAuditLogEntity): Long

    @Query("DELETE FROM tool_audit_logs")
    suspend fun clearAllAuditLogs()
}

@Dao
interface SubagentTaskDao {
    @Query("SELECT * FROM subagent_tasks ORDER BY createdAt DESC")
    fun getAllSubagents(): Flow<List<SubagentTaskEntity>>

    @Query("SELECT * FROM subagent_tasks WHERE id = :id")
    suspend fun getSubagentById(id: String): SubagentTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubagent(task: SubagentTaskEntity)

    @Update
    suspend fun updateSubagent(task: SubagentTaskEntity)

    @Query("DELETE FROM subagent_tasks WHERE id = :id")
    suspend fun deleteSubagent(id: String)
}

@Dao
interface AutonomousGoalDao {
    @Query("SELECT * FROM autonomous_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<AutonomousGoalEntity>>

    @Query("SELECT * FROM autonomous_goals WHERE id = :id")
    suspend fun getGoalById(id: String): AutonomousGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: AutonomousGoalEntity)

    @Update
    suspend fun updateGoal(goal: AutonomousGoalEntity)

    @Query("DELETE FROM autonomous_goals WHERE id = :id")
    suspend fun deleteGoal(id: String)
}

@Dao
interface McpServerDao {
    @Query("SELECT * FROM mcp_servers ORDER BY addedAt DESC")
    fun getAllMcpServers(): Flow<List<McpServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMcpServer(server: McpServerEntity)

    @Update
    suspend fun updateMcpServer(server: McpServerEntity)

    @Query("DELETE FROM mcp_servers WHERE id = :id")
    suspend fun deleteMcpServer(id: String)
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettingEntity>>

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingEntity)

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun removeSetting(key: String)
}
