package com.example.engine.tools

import android.content.Context
import com.example.data.model.RiskLevel
import com.example.data.repository.HermesRepository

data class ToolExecutionResult(
    val isSuccess: Boolean,
    val resultJson: String,
    val summary: String,
    val error: String? = null
)

interface HermesTool {
    val name: String
    val displayName: String
    val description: String
    val category: String
    val riskLevel: RiskLevel
    val requiredPermissions: List<String>
    val parameterSchema: String

    suspend fun execute(
        context: Context,
        params: Map<String, Any?>,
        repository: HermesRepository
    ): ToolExecutionResult
}
