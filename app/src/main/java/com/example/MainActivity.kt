package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.TopAgentAppBar
import com.example.ui.navigation.MainNavigationTab
import com.example.ui.screens.AutomationsGoalsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.GatewaySettingsScreen
import com.example.ui.screens.MemorySkillsScreen
import com.example.ui.screens.ToolMatrixScreen
import com.example.ui.theme.HermesAmber
import com.example.ui.theme.HermesMobileTheme
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.HermesViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HermesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HermesMobileTheme {
                HermesAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HermesAppRoot(viewModel: HermesViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainNavigationTab.CHAT) }
    val snackbarHostState = remember { SnackbarHostState() }

    val isKillSwitchActive by viewModel.isKillSwitchActive.collectAsState()
    val isAgentBusy by viewModel.isAgentBusy.collectAsState()
    val activePersonality by viewModel.activePersonality.collectAsState()
    val selectedModelProvider by viewModel.selectedModelProvider.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    // Request necessary runtime permissions on first startup
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Slate950,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAgentAppBar(
                isKillSwitchActive = isKillSwitchActive,
                isAgentBusy = isAgentBusy,
                onToggleKillSwitch = { viewModel.toggleKillSwitch() },
                onOpenPersonalityMenu = { currentTab = MainNavigationTab.SETTINGS },
                activePersonalityName = activePersonality.name,
                activeModelBadge = selectedModelProvider.displayName
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Slate900,
                contentColor = HermesAmber,
                tonalElevation = 8.dp
            ) {
                MainNavigationTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) HermesAmber else Color(0xFF64748B)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp,
                                    color = if (isSelected) HermesAmber else Color(0xFF64748B)
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HermesAmber,
                            unselectedIconColor = Color(0xFF64748B),
                            indicatorColor = Slate800
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate950)
        ) {
            when (currentTab) {
                MainNavigationTab.CHAT -> ChatScreen(viewModel = viewModel)
                MainNavigationTab.TOOLS -> ToolMatrixScreen(viewModel = viewModel)
                MainNavigationTab.MEMORY -> MemorySkillsScreen(viewModel = viewModel)
                MainNavigationTab.AUTOMATIONS -> AutomationsGoalsScreen(viewModel = viewModel)
                MainNavigationTab.SETTINGS -> GatewaySettingsScreen(viewModel = viewModel)
            }
        }
    }
}

