package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AgentPersonality
import com.example.data.model.ModelProviderType
import com.example.data.repository.HermesRepository
import com.example.engine.agent.AutonomousAgentEngine
import com.example.engine.gateway.LocalHttpServer
import com.example.engine.gateway.TelegramGateway
import com.example.engine.model.ModelService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HermesAgentService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var repository: HermesRepository
    private lateinit var modelService: ModelService
    private lateinit var agentEngine: AutonomousAgentEngine
    private lateinit var telegramGateway: TelegramGateway
    private lateinit var localHttpServer: LocalHttpServer

    private var cronJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HermesAgentService onCreate")

        repository = HermesRepository.getInstance(this)
        modelService = ModelService(repository)
        agentEngine = AutonomousAgentEngine(this, repository, modelService)
        telegramGateway = TelegramGateway(this, repository, agentEngine)
        localHttpServer = LocalHttpServer(repository, agentEngine)

        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildForegroundNotification("Hermes Agent active | Standby"))

        // Start background sub-systems
        startCronEngine()
        telegramGateway.startPolling(serviceScope)

        serviceScope.launch(Dispatchers.IO) {
            val isHttpEnabled = repository.getSetting("local_http_enabled", "true").toBoolean()
            val port = repository.getSetting("local_http_port", "8080").toIntOrNull() ?: 8080
            if (isHttpEnabled) {
                localHttpServer.start(serviceScope, port)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            serviceScope.launch(Dispatchers.IO) {
                repository.setKillSwitch(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun startCronEngine() {
        cronJob?.cancel()
        cronJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    val dueTasks = repository.getDueTasks(now)

                    for (task in dueTasks) {
                        Log.d(TAG, "Executing scheduled automation task: ${task.title}")
                        updateNotification("Running cron: ${task.title}")

                        val steps = agentEngine.processUserTurn(
                            conversationId = "cron_scheduler",
                            userPrompt = task.naturalLanguagePrompt,
                            provider = ModelProviderType.GEMINI,
                            personality = AgentPersonality.DEFAULT_PERSONALITIES.first()
                        )

                        val summary = steps.lastOrNull()?.replyText ?: "Completed"

                        // Calculate next run time (default interval + 1 hour or interval parse)
                        val intervalMs = if (task.cronExpression.startsWith("interval:")) {
                            (task.cronExpression.removePrefix("interval:").toLongOrNull() ?: 60) * 60_000L
                        } else {
                            3600_000L // 1 hour default
                        }

                        val updated = task.copy(
                            lastRunTime = now,
                            nextRunTime = now + intervalMs,
                            lastResultSummary = summary.take(200)
                        )
                        repository.updateScheduledTask(updated)
                    }
                    updateNotification("Hermes Agent active | Standby")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in cron engine loop: ${e.message}")
                }
                delay(30_000) // Check every 30 seconds
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hermes Autonomous Agent Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Hermes autonomous agent alive for persistence, scheduled automations, and gateway connectivity."
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                "hermes_agent_notifications",
                "Hermes Alerts & Actions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task completions, approvals, and high-priority autonomous updates."
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            manager?.createNotificationChannel(alertsChannel)
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, HermesAgentService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hermes Autonomous Agent")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingOpenIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Kill Switch", pendingStopIntent)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildForegroundNotification(statusText))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HermesAgentService onDestroy")
        telegramGateway.stopPolling()
        localHttpServer.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TAG = "HermesAgentService"
        const val CHANNEL_ID = "hermes_persistent_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, HermesAgentService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
