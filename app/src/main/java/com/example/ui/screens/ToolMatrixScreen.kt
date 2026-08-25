package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entities.ToolAuditLogEntity
import com.example.data.model.ApprovalStatus
import com.example.data.model.RiskLevel
import com.example.engine.tools.HermesTool
import com.example.ui.theme.HermesAmber
import com.example.ui.theme.HermesCyan
import com.example.ui.theme.HermesGreen
import com.example.ui.theme.HermesRed
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.HermesViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolMatrixScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val auditLogs by viewModel.auditLogs.collectAsState()
    val allTools = remember { viewModel.toolRegistry.getAllTools() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate900,
            contentColor = HermesAmber,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTab),
                    color = HermesAmber
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Registered Tools (${allTools.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Audit Trail (${auditLogs.size})", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            RegisteredToolsTab(
                tools = allTools,
                viewModel = viewModel
            )
        } else {
            AuditTrailTab(
                auditLogs = auditLogs,
                onClearLogs = { viewModel.clearAuditLogs() }
            )
        }
    }
}

@Composable
fun RegisteredToolsTab(
    tools: List<HermesTool>,
    viewModel: HermesViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredTools = remember(tools, searchQuery) {
        if (searchQuery.isBlank()) tools
        else tools.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search OS capabilities & tools...", color = Color(0xFF64748B), fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedBorderColor = HermesAmber,
                unfocusedBorderColor = Slate800,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredTools, key = { it.name }) { tool ->
                ToolItemCard(
                    tool = tool,
                    onTestRun = {
                        viewModel.sendMessage("Run diagnostic test for tool '${tool.name}'")
                    }
                )
            }
        }
    }
}

@Composable
fun ToolItemCard(
    tool: HermesTool,
    onTestRun: () -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(true) }
    var autoApprove by remember { mutableStateOf(false) }

    val allPermsGranted = remember(tool.requiredPermissions) {
        tool.requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (tool.riskLevel) {
                                    RiskLevel.LOW -> HermesGreen.copy(alpha = 0.2f)
                                    RiskLevel.MEDIUM -> HermesAmber.copy(alpha = 0.2f)
                                    RiskLevel.HIGH -> HermesRed.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tool.riskLevel.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when (tool.riskLevel) {
                                    RiskLevel.LOW -> HermesGreen
                                    RiskLevel.MEDIUM -> HermesAmber
                                    RiskLevel.HIGH -> HermesRed
                                }
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tool.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HermesAmber,
                        checkedTrackColor = Slate800,
                        uncheckedThumbColor = Slate700,
                        uncheckedTrackColor = Slate950
                    ),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tool.displayName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
            )

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Slate950,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: ${tool.name}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = HermesCyan)
                    )

                    Text(
                        text = if (tool.requiredPermissions.isEmpty()) "No permissions needed"
                        else if (allPermsGranted) "✓ Permissions Granted"
                        else "⚠️ Needs Manifest Permission",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (allPermsGranted) HermesGreen else HermesAmber
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tool.riskLevel == RiskLevel.HIGH) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = autoApprove,
                            onCheckedChange = { autoApprove = it },
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto-Approve",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0))
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = onTestRun,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = HermesAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AuditTrailTab(
    auditLogs: List<ToolAuditLogEntity>,
    onClearLogs: () -> Unit
) {
    var selectedLogForModal by remember { mutableStateOf<ToolAuditLogEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "IMMUTABLE AUDIT LOGS",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), letterSpacing = 1.sp)
            )

            if (auditLogs.isNotEmpty()) {
                IconButton(onClick = onClearLogs) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Logs", tint = Color(0xFFEF4444))
                }
            }
        }

        if (auditLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No tool invocations recorded yet.", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(auditLogs, key = { it.id }) { log ->
                    AuditLogItemCard(
                        log = log,
                        onClick = { selectedLogForModal = log }
                    )
                }
            }
        }
    }

    if (selectedLogForModal != null) {
        val log = selectedLogForModal!!
        AlertDialog(
            onDismissRequest = { selectedLogForModal = null },
            containerColor = Slate900,
            title = {
                Text(
                    text = "Audit Log Entry #${log.id}",
                    style = MaterialTheme.typography.titleMedium.copy(color = HermesAmber, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Tool: ${log.toolName} [${log.riskLevel.displayName}]", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Triggered By: ${log.triggeredBy}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("Duration: ${log.executionDurationMs}ms", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("Status: ${log.approvalStatus.displayName}", color = HermesGreen, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("INPUT JSON:", color = HermesCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Surface(color = Slate950, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = formatPrettyJson(log.inputJson),
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("OUTPUT JSON:", color = HermesAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Surface(color = Slate950, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = formatPrettyJson(log.outputJson),
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLogForModal = null }) {
                    Text("Close", color = HermesAmber)
                }
            }
        )
    }
}

@Composable
fun AuditLogItemCard(
    log: ToolAuditLogEntity,
    onClick: () -> Unit
) {
    val timeFormatted = remember(log.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.toolName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = HermesAmber
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (log.approvalStatus == ApprovalStatus.REJECTED) HermesRed.copy(alpha = 0.2f)
                                else HermesGreen.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.approvalStatus.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (log.approvalStatus == ApprovalStatus.REJECTED) HermesRed else HermesGreen,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
                Text(
                    text = "Triggered by: ${log.triggeredBy} • ${log.executionDurationMs}ms",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                )
            }

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
            )
        }
    }
}

private fun formatPrettyJson(json: String): String {
    return try {
        if (json.startsWith("{")) JSONObject(json).toString(2)
        else if (json.startsWith("[")) org.json.JSONArray(json).toString(2)
        else json
    } catch (_: Exception) {
        json
    }
}
