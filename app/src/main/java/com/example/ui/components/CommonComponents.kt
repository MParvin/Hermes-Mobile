package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.RiskLevel
import com.example.ui.theme.HermesAmber
import com.example.ui.theme.HermesCyan
import com.example.ui.theme.HermesGreen
import com.example.ui.theme.HermesRed
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import org.json.JSONObject

@Composable
fun TopAgentAppBar(
    isKillSwitchActive: Boolean,
    isAgentBusy: Boolean,
    onToggleKillSwitch: () -> Unit,
    onOpenPersonalityMenu: () -> Unit,
    activePersonalityName: String,
    activeModelBadge: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Slate950,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenPersonalityMenu() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isKillSwitchActive) HermesRed
                                else if (isAgentBusy) HermesAmber
                                else HermesGreen
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "HERMES MOBILE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = HermesAmber
                            )
                        )
                        Text(
                            text = "$activePersonalityName • $activeModelBadge",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }

                // Kill Switch Action Button
                Button(
                    onClick = onToggleKillSwitch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isKillSwitchActive) HermesGreen else HermesRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("kill_switch_button")
                ) {
                    Icon(
                        imageVector = if (isKillSwitchActive) Icons.Default.PlayArrow else Icons.Default.Warning,
                        contentDescription = "Kill Switch",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isKillSwitchActive) "RESUME" else "KILL SWITCH",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun ThoughtCollapseCard(thoughts: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🧠 Reasoning & Internal Thought",
                        style = MaterialTheme.typography.labelMedium.copy(color = HermesCyan, fontWeight = FontWeight.SemiBold)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Thoughts",
                    tint = HermesCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = thoughts,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ToolExecutionCard(
    toolName: String,
    toolCallJson: String?,
    toolResultJson: String?
) {
    var isDetailsOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B)))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDetailsOpen = !isDetailsOpen },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(HermesAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TOOL: $toolName",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = HermesAmber
                        )
                    )
                }
                Text(
                    text = if (isDetailsOpen) "Hide JSON" else "View Output",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                )
            }

            AnimatedVisibility(visible = isDetailsOpen) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (!toolCallJson.isNullOrBlank()) {
                        Text(
                            text = "Parameters:",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                        )
                        Text(
                            text = formatJsonPreview(toolCallJson),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE2E8F0)
                            )
                        )
                    }
                    if (!toolResultJson.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Result Evidence:",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                        )
                        Text(
                            text = formatJsonPreview(toolResultJson),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = HermesGreen
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HighRiskApprovalCard(
    toolName: String,
    paramsJson: String,
    messageId: String,
    onApprove: (Map<String, Any?>) -> Unit,
    onReject: () -> Unit
) {
    val paramsMap = remember(paramsJson) {
        val map = mutableMapOf<String, Any?>()
        try {
            val root = JSONObject(paramsJson)
            val p = root.optJSONObject("parameters") ?: root
            val keys = p.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = p.get(k)
            }
        } catch (_: Exception) {}
        map
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, HermesRed, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "High Risk Action",
                    tint = HermesRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AUTHORIZATION REQUIRED",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hermes requests permission to execute high-risk OS capability:",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFECACA))
            )

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Slate950,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Tool: $toolName",
                        style = MaterialTheme.typography.labelMedium.copy(color = HermesAmber, fontWeight = FontWeight.Bold)
                    )
                    paramsMap.forEach { (k, v) ->
                        Text(
                            text = "$k: $v",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("reject_tool_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deny")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = { onApprove(paramsMap) },
                    colors = ButtonDefaults.buttonColors(containerColor = HermesRed, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("approve_tool_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Authorize & Run")
                }
            }
        }
    }
}

private fun formatJsonPreview(json: String): String {
    return try {
        if (json.startsWith("{")) {
            JSONObject(json).toString(2)
        } else if (json.startsWith("[")) {
            org.json.JSONArray(json).toString(2)
        } else {
            json
        }
    } catch (_: Exception) {
        json
    }
}
