package com.example.data.model

enum class RiskLevel(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

enum class ModelProviderType(val displayName: String, val defaultModel: String) {
    GEMINI("Gemini 2.5 Pro / Flash", "gemini-2.5-flash"),
    CLAUDE("Anthropic Claude 3.5 Sonnet", "claude-3-5-sonnet-20241022"),
    OPENAI("OpenAI GPT-4o / o3-mini", "gpt-4o"),
    OPENROUTER("OpenRouter Multi-Gateway", "nousresearch/hermes-3-llama-3.1-405b"),
    LOCAL_CUSTOM("Local / Ollama / LM Studio", "hermes-3-llama-3.1-8b"),
    MOA_MIXTURE("Mixture of Agents (MoA)", "moa-ensemble-v1")
}

enum class MemoryCategory(val displayName: String) {
    PREFERENCE("User Preferences"),
    FACT("Learned Facts"),
    WORK("Work & Projects"),
    CONTACT("Contacts & People"),
    SYSTEM("System & Environment")
}

enum class TaskStatus(val displayName: String) {
    PENDING("Pending"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

enum class ApprovalStatus(val displayName: String) {
    PENDING("Pending Approval"),
    APPROVED("Approved by Owner"),
    REJECTED("Rejected by Owner"),
    AUTO_APPROVED("Auto-Approved")
}

enum class DeliverChannel(val displayName: String) {
    IN_APP("In-App Only"),
    TELEGRAM("Telegram Bot"),
    LOCAL_API("Local HTTP API"),
    ALL("Deliver to All Channels")
}

data class AgentPersonality(
    val id: String,
    val name: String,
    val tagline: String,
    val systemPrompt: String
) {
    companion object {
        val DEFAULT_PERSONALITIES = listOf(
            AgentPersonality(
                id = "hermes_core",
                name = "Hermes Autonomous",
                tagline = "Uncensored, precise, proactive tool orchestration",
                systemPrompt = """You are Hermes, an autonomous on-device AI agent operating natively on an Android device.
You have direct access to a tool registry containing Android OS capabilities (SMS, calls, camera, contacts, calendar, files, telemetry, web search, memory, cron scheduling, subagents, and goals).
When asked to perform tasks, select and invoke the appropriate tools using structured tool calls.
Be concise, proactive, factual, and execute multi-step plans with relentless focus on verified evidence."""
            ),
            AgentPersonality(
                id = "cyber_sec",
                name = "SecOps Guardian",
                tagline = "Security auditor, permission watchdog, threat hunter",
                systemPrompt = """You are Hermes SecOps, a cybersecurity and privacy specialist.
Audit tool calls, enforce strict permission boundaries, analyze device logs, and verify all external network calls and data flows."""
            ),
            AgentPersonality(
                id = "sys_architect",
                name = "Android SysAdmin",
                tagline = "Deep system automation, storage, and device telemetry",
                systemPrompt = """You are Hermes SysAdmin, an expert in Android automation, batch processing, WorkManager scheduling, and OS resource management."""
            ),
            AgentPersonality(
                id = "creative_coder",
                name = "Code & Skills Engineer",
                tagline = "Autonomous scriptwriter and self-writing skill creator",
                systemPrompt = """You are Hermes Engineer. You synthesize complex routines into persistent self-writing skills, inspect source code, calculate solutions, and formulate autonomous multi-step execution plans."""
            )
        )
    }
}
