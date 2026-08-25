package com.example.ui.screens

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.FactMemoryEntity
import com.example.data.local.entities.SkillEntity
import com.example.data.model.MemoryCategory
import com.example.ui.theme.HermesAmber
import com.example.ui.theme.HermesCyan
import com.example.ui.theme.HermesGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.HermesViewModel

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorySkillsScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val memories by viewModel.memories.collectAsState()
    val skills by viewModel.skills.collectAsState()

    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showAddSkillDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddMemoryDialog = true
                    else showAddSkillDialog = true
                },
                containerColor = HermesAmber,
                contentColor = Slate950,
                shape = CircleShape,
                modifier = Modifier.testTag("add_memory_skill_fab")
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
                    text = { Text("Long-Term Memories (${memories.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Synthesized Skills (${skills.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                MemoriesTabContent(
                    memories = memories,
                    onDelete = { viewModel.deleteMemory(it) }
                )
            } else {
                SkillsTabContent(
                    skills = skills,
                    onRunSkill = { viewModel.runSkill(it) },
                    onDeleteSkill = { viewModel.deleteSkill(it) }
                )
            }
        }
    }

    if (showAddMemoryDialog) {
        AddMemoryDialog(
            onDismiss = { showAddMemoryDialog = false },
            onSave = { subject, content, cat ->
                viewModel.addMemory(subject, content, cat)
                showAddMemoryDialog = false
            }
        )
    }

    if (showAddSkillDialog) {
        AddSkillDialog(
            onDismiss = { showAddSkillDialog = false },
            onSave = { name, displayName, desc, instructions ->
                viewModel.sendMessage("Save synthesized skill '$displayName': $instructions")
                showAddSkillDialog = false
            }
        )
    }
}

@Composable
fun MemoriesTabContent(
    memories: List<FactMemoryEntity>,
    onDelete: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(memories, searchQuery) {
        if (searchQuery.isBlank()) memories
        else memories.filter {
            it.subject.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true) ||
                    it.category.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search learned facts & preferences...", color = Color(0xFF64748B), fontSize = 13.sp) },
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

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No memories found. The agent learns facts automatically or add them via + button.", color = Color(0xFF64748B), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { mem ->
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
                                            .background(HermesCyan.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = mem.category.displayName.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(color = HermesCyan, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = mem.subject,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                }

                                IconButton(onClick = { onDelete(mem.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mem.content,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCBD5E1))
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Source: ${mem.source} • Confidence: ${(mem.confidence * 100).toInt()}%",
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
fun SkillsTabContent(
    skills: List<SkillEntity>,
    onRunSkill: (SkillEntity) -> Unit,
    onDeleteSkill: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "SELF-WRITING AUTONOMOUS SKILLS",
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), letterSpacing = 1.sp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (skills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No synthesized skills yet. Teach Hermes new workflows by saying 'Save a skill to...'", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(skills, key = { it.name }) { skill ->
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
                                        text = skill.displayName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = HermesAmber)
                                    )
                                    Text(
                                        text = "Trigger: \"${skill.triggerPattern}\"",
                                        style = MaterialTheme.typography.labelSmall.copy(color = HermesCyan, fontFamily = FontFamily.Monospace)
                                    )
                                }

                                IconButton(onClick = { onDeleteSkill(skill.name) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = skill.description,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(color = Slate950, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Instructions:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                                    Text(
                                        text = skill.instructions,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE2E8F0), fontFamily = FontFamily.Monospace)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Invoked ${skill.invocationCount} times",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                )

                                Button(
                                    onClick = { onRunSkill(skill) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HermesAmber, contentColor = Slate950),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Execute Skill", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, MemoryCategory) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(MemoryCategory.FACT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Text("Store Long-Term Fact", style = MaterialTheme.typography.titleMedium.copy(color = HermesAmber, fontWeight = FontWeight.Bold))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Work Preferences)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content / Knowledge") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && content.isNotBlank()) {
                        onSave(subject, content, selectedCat)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HermesAmber, contentColor = Slate950)
            ) {
                Text("Save Fact")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        }
    )
}

@Composable
fun AddSkillDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Text("Synthesize Custom Skill", style = MaterialTheme.typography.titleMedium.copy(color = HermesAmber, fontWeight = FontWeight.Bold))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Skill Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Natural Language Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && instructions.isNotBlank()) {
                        onSave(name.lowercase().replace(" ", "_"), name, desc, instructions)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HermesAmber, contentColor = Slate950)
            ) {
                Text("Synthesize")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF94A3B8)) }
        }
    )
}
