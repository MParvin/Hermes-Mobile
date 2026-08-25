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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AutonomousGoalEntity
import com.example.data.local.entities.ScheduledTaskEntity
import com.example.data.local.entities.SubagentTaskEntity
import com.example.data.model.DeliverChannel
import com.example.data.model.TaskStatus
import com.example.ui.theme.HermesAmber
import com.example.ui.theme.HermesCyan
import com.example.ui.theme.HermesGreen
import com.example.ui.theme.HermesRed
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.HermesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationsGoalsScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scheduledTasks by viewModel.scheduledTasks.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val subagents by viewModel.subagents.collectAsState()

    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showSpawnSubagentDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showAddScheduleDialog = true
                        1 -> showAddGoalDialog = true
                        2 -> showSpawnSubagentDialog = true
                    }
                },
                containerColor = HermesAmber,
                contentColor = Slate950,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    text = { Text("Cron Jobs (${scheduledTasks.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Autonomous Goals (${goals.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Subagents (${subagents.size})", fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedTab) {
                0 -> ScheduledTasksTabContent(
                    tasks = scheduledTasks,
                    onDelete = { viewModel.deleteScheduledTask(it) }
                )
                1 -> GoalsTabContent(
                    goals = goals
                )
                2 -> SubagentsTabContent(
                    subagents = subagents
                )
            }
        }
    }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showAddScheduleDialog = false },
            onSave = { title, prompt, cron, channel ->
                viewModel.createScheduledTask(title, prompt, cron, channel)
                showAddScheduleDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onSave = { objective, criteria ->
                viewModel.createAutonomousGoal(objective, criteria)
                showAddGoalDialog = false
            }
        )
    }

    if (showSpawnSubagentDialog) {
        SpawnSubagentDialog(
            onDismiss = { showSpawnSubagentDialog = false },
            onSave = { title, objective ->
                viewModel.spawnSubagent(title, objective)
                showSpawnSubagentDialog = false
            }
        )
    }
}

@Composable
fun ScheduledTasksTabContent(
    tasks: List<ScheduledTaskEntity>,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "PERSISTENT BACKGROUND AUTOMATIONS",
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), letterSpacing = 1.sp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No cron automations scheduled. Tap + to set a recurring task.", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                    Text(
                                        text = "Schedule: ${task.cronExpression} • Channel: ${task.targetChannel.displayName}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = HermesAmber, fontFamily = FontFamily.Monospace)
                                    )
                                }

                                IconButton(onClick = { onDelete(task.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Prompt: ${task.naturalLanguagePrompt}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
                            )

                            if (!task.lastResultSummary.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(color = Slate950, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Last Result: ${task.lastResultSummary}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = HermesGreen),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsTabContent(goals: List<AutonomousGoalEntity>) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "AUTONOMOUS GOAL & VERIFICATION LOOPS",
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), letterSpacing = 1.sp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active autonomous goals. Launch a goal loop with + button.", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(goals, key = { it.id }) { goal ->
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
                                Text(
                                    text = goal.objective,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = HermesAmber)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (goal.status == TaskStatus.RUNNING) HermesCyan.copy(alpha = 0.2f) else HermesGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = goal.status.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (goal.status == TaskStatus.RUNNING) HermesCyan else HermesGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Success Criteria: ${goal.successCriteria}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
                            )
                            Text(
                                text = "Iteration: ${goal.currentIteration} / ${goal.maxIterations}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(color = Slate950, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Evidence: ${goal.evidenceLogs}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = HermesGreen, fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubagentsTabContent(subagents: List<SubagentTaskEntity>) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "PARALLEL BACKGROUND SUBAGENTS",
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), letterSpacing = 1.sp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (subagents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No parallel subagents active.", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subagents, key = { it.id }) { sub ->
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
                                Text(
                                    text = sub.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Text(
                                    text = sub.assignedModel,
                                    style = MaterialTheme.typography.labelSmall.copy(color = HermesCyan, fontFamily = FontFamily.Monospace)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sub.objective,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { sub.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = HermesAmber,
                                trackColor = Slate950
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Status: ${sub.status.displayName} (${sub.progressPercent}%)",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, DeliverChannel) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var cron by remember { mutableStateOf("interval:60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Text("Create Scheduled Automation", style = MaterialTheme.typography.titleMedium.copy(color = HermesAmber, fontWeight = FontWeight.Bold))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title (e.g. Battery Watchdog)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Agent Prompt to Execute") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cron,
                    onValueChange = { cron = it },
                    label = { Text("Interval/Cron (e.g. interval:60)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && prompt.isNotBlank()) {
                        onSave(title, prompt, cron, DeliverChannel.ALL)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HermesAmber, contentColor = Slate950)
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        }
    )
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var obj by remember { mutableStateOf("") }
    var crit by remember { mutableStateOf("Verified evidence gathered") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Text("Launch Autonomous Goal Loop", style = MaterialTheme.typography.titleMedium.copy(color = HermesAmber, fontWeight = FontWeight.Bold))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = obj,
                    onValueChange = { obj = it },
                    label = { Text("Goal Objective") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = crit,
                    onValueChange = { crit = it },
                    label = { Text("Judge Success Criteria") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (obj.isNotBlank()) {
                        onSave(obj, crit)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HermesAmber, contentColor = Slate950)
            ) {
                Text("Launch Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        }
    )
}

@Composable
fun SpawnSubagentDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var obj by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Text("Spawn Parallel Subagent", style = MaterialTheme.typography.titleMedium.copy(color = HermesAmber, fontWeight = FontWeight.Bold))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Subagent Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = obj,
                    onValueChange = { obj = it },
                    label = { Text("Subagent Objective") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && obj.isNotBlank()) {
                        onSave(title, obj)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HermesAmber, contentColor = Slate950)
            ) {
                Text("Spawn")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        }
    )
}
