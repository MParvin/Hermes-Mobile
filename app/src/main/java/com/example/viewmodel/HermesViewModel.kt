package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.AutonomousGoalEntity
import com.example.data.local.entities.ConversationEntity
import com.example.data.local.entities.FactMemoryEntity
import com.example.data.local.entities.McpServerEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.ScheduledTaskEntity
import com.example.data.local.entities.SkillEntity
import com.example.data.local.entities.SubagentTaskEntity
import com.example.data.local.entities.ToolAuditLogEntity
import com.example.data.model.AgentPersonality
import com.example.data.model.DeliverChannel
import com.example.data.model.MemoryCategory
import com.example.data.model.ModelProviderType
import com.example.data.model.TaskStatus
import com.example.data.repository.HermesRepository
import com.example.engine.agent.AutonomousAgentEngine
import com.example.engine.model.ModelService
import com.example.engine.tools.ToolRegistry
import com.example.service.HermesAgentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

import kotlinx.coroutines.flow.map

class HermesViewModel(application: Application) : AndroidViewModel(application) {

    val repository = HermesRepository.getInstance(application)
    private val modelService = ModelService(repository)
    private val agentEngine = AutonomousAgentEngine(application, repository, modelService)
    val toolRegistry = ToolRegistry.instance

    private var textToSpeech: TextToSpeech? = null
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _currentConversationId = MutableStateFlow("default_session")
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    private val _activePersonality = MutableStateFlow(AgentPersonality.DEFAULT_PERSONALITIES.first())
    val activePersonality: StateFlow<AgentPersonality> = _activePersonality.asStateFlow()

    private val _selectedModelProvider = MutableStateFlow(ModelProviderType.GEMINI)
    val selectedModelProvider: StateFlow<ModelProviderType> = _selectedModelProvider.asStateFlow()

    private val _isAgentBusy = MutableStateFlow(false)
    val isAgentBusy: StateFlow<Boolean> = _isAgentBusy.asStateFlow()

    private val _isKillSwitchActive = MutableStateFlow(false)
    val isKillSwitchActive: StateFlow<Boolean> = _isKillSwitchActive.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Data streams from Room
    val allSettings: StateFlow<Map<String, String>> = repository.getAllSettings()
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val conversations: StateFlow<List<ConversationEntity>> = repository.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<MessageEntity>> = _currentConversationId.flatMapLatest { convId ->
        repository.getMessages(convId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<FactMemoryEntity>> = repository.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val skills: StateFlow<List<SkillEntity>> = repository.getAllSkills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledTasks: StateFlow<List<ScheduledTaskEntity>> = repository.getAllScheduledTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<ToolAuditLogEntity>> = repository.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subagents: StateFlow<List<SubagentTaskEntity>> = repository.getAllSubagents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<AutonomousGoalEntity>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mcpServers: StateFlow<List<McpServerEntity>> = repository.getAllMcpServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Start Foreground Service
        HermesAgentService.startService(application)

        // Init TextToSpeech for Voice Mode
        textToSpeech = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                _isTtsReady.value = true
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isKillSwitchActive.value = repository.isKillSwitchActive()

            val savedProvider = repository.getSetting("active_provider", "GEMINI")
            try {
                _selectedModelProvider.value = ModelProviderType.valueOf(savedProvider)
            } catch (_: Exception) {}

            // Initialize default conversation if empty
            val conv = repository.getConversation("default_session")
            if (conv == null) {
                repository.saveConversation(
                    ConversationEntity(
                        id = "default_session",
                        title = "Main Autonomous Chat"
                    )
                )
            }

            // Seed initial memory facts & skills if first launch
            seedInitialData()
        }
    }

    private suspend fun seedInitialData() {
        if (repository.getTopMemoriesForContext().isEmpty()) {
            repository.saveMemory(
                category = MemoryCategory.SYSTEM,
                subject = "Operating Environment",
                content = "Hermes running on Android 12+ with direct OS tool-calling permissions and local Room persistence.",
                source = "System Init"
            )
            repository.saveMemory(
                category = MemoryCategory.PREFERENCE,
                subject = "Execution Protocol",
                content = "High-risk actions require interactive approval card or explicit auto-approve toggle.",
                source = "System Init"
            )
        }

        if (repository.getSkill("morning_briefing") == null) {
            repository.saveSkill(
                SkillEntity(
                    name = "morning_briefing",
                    displayName = "Morning Executive Briefing",
                    description = "Collects device battery, upcoming calendar events, recent SMS, and latest news summary.",
                    triggerPattern = "morning briefing",
                    instructions = "Query calendar events for today, fetch telemetry, check SMS inbox, and summarize into an executive briefing card.",
                    toolSequenceJson = """["calendar_events", "get_device_telemetry", "read_sms", "web_search"]""",
                    isAutoSaved = false
                )
            )
        }

        if (repository.getSkill("sys_health_audit") == null) {
            repository.saveSkill(
                SkillEntity(
                    name = "sys_health_audit",
                    displayName = "System Telemetry & Health Audit",
                    description = "Audits RAM usage, storage space, battery charge rate, and active network connections.",
                    triggerPattern = "system health audit",
                    instructions = "Run get_device_telemetry and check available storage and battery.",
                    toolSequenceJson = """["get_device_telemetry"]""",
                    isAutoSaved = false
                )
            )
        }
    }

    fun selectConversation(id: String) {
        _currentConversationId.value = id
    }

    fun createNewConversation(title: String = "Autonomous Session") {
        viewModelScope.launch(Dispatchers.IO) {
            val id = "conv_${UUID.randomUUID().toString().take(8)}"
            repository.saveConversation(
                ConversationEntity(
                    id = id,
                    title = title,
                    personalityId = _activePersonality.value.id,
                    modelProvider = _selectedModelProvider.value.name
                )
            )
            _currentConversationId.value = id
        }
    }

    fun setPersonality(personality: AgentPersonality) {
        _activePersonality.value = personality
    }

    fun setModelProvider(provider: ModelProviderType) {
        _selectedModelProvider.value = provider
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("active_provider", provider.name)
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isAgentBusy.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isAgentBusy.value = true
            try {
                val steps = agentEngine.processUserTurn(
                    conversationId = _currentConversationId.value,
                    userPrompt = userText,
                    provider = _selectedModelProvider.value,
                    personality = _activePersonality.value
                )

                val lastReply = steps.lastOrNull()?.replyText
                if (!lastReply.isNullOrBlank() && _isTtsReady.value) {
                    // Optional TTS voice synthesis for short responses
                    // textToSpeech?.speak(lastReply.take(300), TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } catch (e: Exception) {
                _toastMessage.value = "Error executing turn: ${e.message}"
            } finally {
                _isAgentBusy.value = false
            }
        }
    }

    fun approveToolExecution(messageId: String, toolName: String, params: Map<String, Any?>) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAgentBusy.value = true
            try {
                agentEngine.executeApprovedTool(
                    conversationId = _currentConversationId.value,
                    messageId = messageId,
                    toolName = toolName,
                    params = params
                )
                _toastMessage.value = "Tool '$toolName' approved & executed"
            } catch (e: Exception) {
                _toastMessage.value = "Approval failed: ${e.message}"
            } finally {
                _isAgentBusy.value = false
            }
        }
    }

    fun rejectToolExecution(messageId: String, toolName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMessage(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = _currentConversationId.value,
                    sender = "system",
                    content = "🚫 Execution of high-risk tool **$toolName** was rejected by device owner.",
                    timestamp = System.currentTimeMillis()
                )
            )
            _toastMessage.value = "Action '$toolName' rejected"
        }
    }

    fun toggleKillSwitch() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.isKillSwitchActive()
            val newState = !current
            repository.setKillSwitch(newState)
            _isKillSwitchActive.value = newState
            _toastMessage.value = if (newState) "🚨 Global Kill Switch ACTIVATED" else "✅ Agent execution restored"
        }
    }

    fun speakText(text: String) {
        if (_isTtsReady.value && text.isNotBlank()) {
            val clean = text.replace(Regex("""[*#_`]"""), "").take(500)
            textToSpeech?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance")
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    // Memory operations
    fun addMemory(subject: String, content: String, category: MemoryCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMemory(category, subject, content, 1.0f, "User Manual")
            _toastMessage.value = "Memory saved: '$subject'"
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemory(id)
            _toastMessage.value = "Memory deleted"
        }
    }

    // Skill operations
    fun runSkill(skill: SkillEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementSkillUse(skill.name)
            sendMessage("Execute saved skill '${skill.displayName}': ${skill.instructions}")
        }
    }

    fun deleteSkill(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSkill(name)
            _toastMessage.value = "Skill '$name' deleted"
        }
    }

    // Schedule operations
    fun createScheduledTask(title: String, prompt: String, cron: String, channel: DeliverChannel = DeliverChannel.ALL) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = ScheduledTaskEntity(
                id = UUID.randomUUID().toString().take(8),
                title = title,
                naturalLanguagePrompt = prompt,
                cronExpression = cron,
                targetChannel = channel,
                nextRunTime = System.currentTimeMillis() + 60_000L
            )
            repository.saveScheduledTask(task)
            _toastMessage.value = "Scheduled automation '$title' created"
        }
    }

    fun deleteScheduledTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteScheduledTask(id)
            _toastMessage.value = "Automation deleted"
        }
    }

    // Autonomous Goal operations
    fun createAutonomousGoal(objective: String, criteria: String, maxIterations: Int = 5) {
        viewModelScope.launch(Dispatchers.IO) {
            val goalId = "goal_${UUID.randomUUID().toString().take(8)}"
            val goal = AutonomousGoalEntity(
                id = goalId,
                objective = objective,
                successCriteria = criteria,
                maxIterations = maxIterations,
                currentIteration = 1,
                status = TaskStatus.RUNNING,
                evidenceLogs = JSONArray().put(
                    JSONObject().put("step", 1).put("action", "Goal started: $objective").put("timestamp", System.currentTimeMillis())
                ).toString()
            )
            repository.saveGoal(goal)
            _toastMessage.value = "Autonomous goal launched: '$objective'"
            sendMessage("Launch autonomous goal: $objective. Success criteria: $criteria")
        }
    }

    // Subagent spawn
    fun spawnSubagent(title: String, objective: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = "sub_${UUID.randomUUID().toString().take(8)}"
            val sub = SubagentTaskEntity(
                id = id,
                title = title,
                objective = objective,
                assignedModel = _selectedModelProvider.value.name,
                status = TaskStatus.RUNNING,
                progressPercent = 25,
                logs = "Subagent executing in background: $objective\n"
            )
            repository.saveSubagent(sub)
            _toastMessage.value = "Subagent '$title' spawned"
        }
    }

    // Tool settings
    fun setToolEnabled(toolName: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setToolEnabled(toolName, enabled)
        }
    }

    fun setToolAutoApprove(toolName: String, autoApprove: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setToolAutoApprove(toolName, autoApprove)
        }
    }

    fun saveAllSettings(settings: Map<String, String>, toastMessage: String? = "Settings saved successfully") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSettingsBatch(settings)
            if (toastMessage != null) {
                _toastMessage.value = toastMessage
            }
        }
    }

    fun saveSetting(key: String, value: String, showToast: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting(key, value)
            if (showToast) {
                _toastMessage.value = "Setting updated"
            }
        }
    }

    fun setProviderEnabled(provider: ModelProviderType, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setProviderEnabled(provider, enabled)
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAuditLogs()
            _toastMessage.value = "Audit logs cleared"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown()
    }
}
