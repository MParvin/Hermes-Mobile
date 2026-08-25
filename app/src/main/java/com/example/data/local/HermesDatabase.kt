package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.daos.AppSettingDao
import com.example.data.local.daos.AutonomousGoalDao
import com.example.data.local.daos.ConversationDao
import com.example.data.local.daos.FactMemoryDao
import com.example.data.local.daos.McpServerDao
import com.example.data.local.daos.MessageDao
import com.example.data.local.daos.ScheduledTaskDao
import com.example.data.local.daos.SkillDao
import com.example.data.local.daos.SubagentTaskDao
import com.example.data.local.daos.ToolAuditLogDao
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

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        FactMemoryEntity::class,
        SkillEntity::class,
        ScheduledTaskEntity::class,
        ToolAuditLogEntity::class,
        SubagentTaskEntity::class,
        AutonomousGoalEntity::class,
        McpServerEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HermesDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun factMemoryDao(): FactMemoryDao
    abstract fun skillDao(): SkillDao
    abstract fun scheduledTaskDao(): ScheduledTaskDao
    abstract fun toolAuditLogDao(): ToolAuditLogDao
    abstract fun subagentTaskDao(): SubagentTaskDao
    abstract fun autonomousGoalDao(): AutonomousGoalDao
    abstract fun mcpServerDao(): McpServerDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: HermesDatabase? = null

        fun getInstance(context: Context): HermesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HermesDatabase::class.java,
                    "hermes_mobile.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
