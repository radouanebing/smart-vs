package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemSettings
import com.example.ui.screens.DeviceTrackingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SirenTriggerOverlay
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.screens.SuiteScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AssistantViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure activity to wake up screen and display above OS security lockscreens when active
        showOverLockscreen()

        viewModel = ViewModelProvider(this)[AssistantViewModel::class.java]

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAssistantContainer(viewModel)
            }
        }
    }

    companion object {
        var isAppInForeground = false
    }

    override fun onStart() {
        super.onStart()
        isAppInForeground = true
        if (::viewModel.isInitialized) {
            viewModel.refreshBackgroundVoiceService()
        }
    }

    override fun onStop() {
        super.onStop()
        isAppInForeground = false
        if (::viewModel.isInitialized) {
            viewModel.refreshBackgroundVoiceService()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        if (::viewModel.isInitialized) {
            viewModel.refreshBackgroundVoiceService()
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == "com.example.action.WAKE_ASSISTANT") {
            viewModel.onWakeWordDetected()
        }
    }

    private fun showOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
fun MainAssistantContainer(viewModel: AssistantViewModel) {
    val settingsState by viewModel.systemSettings.collectAsState()
    val sirenActiveState by viewModel.findMyPhoneActive.collectAsState()
    
    val settings = settingsState ?: SystemSettings()
    val isRtl = settings.preferredLanguage == "ar" || settings.preferredLanguage == "dz"

    val context = LocalContext.current
    val permissionManager = remember { com.example.permissions.PermissionManager(context) }
    var showOnboarding by remember { mutableStateOf(permissionManager.isFirstLaunch()) }

    var currentTab by remember { mutableStateOf("home") }

    // Immersive Theme Colors
    val cardBgColor = Color(0xFF161922)
    val appBackground = Color(0xFF0F1117)

    Box(modifier = Modifier.fillMaxSize().background(appBackground)) {
        Scaffold(
            bottomBar = {
                // If finding alarm is active, do not display navigation tabs
                if (!sirenActiveState) {
                    CustomNavigationBar(
                        currentTab = currentTab,
                        isRtl = isRtl,
                        onTabSelected = { currentTab = it },
                        cardBgColor = cardBgColor
                    )
                }
            },
            containerColor = appBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    "home" -> VoiceAssistantScreen(
                        viewModel = viewModel,
                        settings = settings,
                        onNavigateToTracking = { currentTab = "tracking" }
                    )
                    "suite" -> SuiteScreen(
                        viewModel = viewModel,
                        settings = settings
                    )
                    "tracking" -> DeviceTrackingScreen(
                        viewModel = viewModel,
                        settings = settings
                    )
                    "settings" -> SettingsScreen(
                        viewModel = viewModel,
                        settings = settings
                    )
                }
            }
        }

        // Full-screen Alert Siren Overlay
        if (sirenActiveState) {
            SirenTriggerOverlay(
                viewModel = viewModel,
                isRtl = isRtl,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Full-screen Guided Onboarding Wizard
        if (showOnboarding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appBackground)
            ) {
                com.example.permissions.PermissionWizardScreen(
                    manager = permissionManager,
                    initialLanguage = settings.preferredLanguage,
                    onWizardComplete = {
                        showOnboarding = false
                    }
                )
            }
        }
    }
}

@Composable
fun CustomNavigationBar(
    currentTab: String,
    isRtl: Boolean,
    onTabSelected: (String) -> Unit,
    cardBgColor: Color
) {
    Surface(
        color = cardBgColor,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(cardBgColor),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Assistant Tab
                NavigationTabItem(
                    selected = currentTab == "home",
                    label = if (isRtl) "الرئيسية" else "Talk",
                    iconSelected = Icons.Filled.Home,
                    iconUnselected = Icons.Outlined.Home,
                    onClick = { onTabSelected("home") },
                    testTag = "tab_home"
                )

                // Suite Tab
                NavigationTabItem(
                    selected = currentTab == "suite",
                    label = if (isRtl) "المنصة" else "Suite",
                    iconSelected = Icons.Filled.Dashboard,
                    iconUnselected = Icons.Outlined.Dashboard,
                    onClick = { onTabSelected("suite") },
                    testTag = "tab_suite"
                )

                // Remote Device Status Tab
                NavigationTabItem(
                    selected = currentTab == "tracking",
                    label = if (isRtl) "التتبع" else "Devices",
                    iconSelected = Icons.Filled.MyLocation,
                    iconUnselected = Icons.Outlined.MyLocation,
                    onClick = { onTabSelected("tracking") },
                    testTag = "tab_tracking"
                )

                // Configuration Settings Tab
                NavigationTabItem(
                    selected = currentTab == "settings",
                    label = if (isRtl) "الإعدادات" else "Settings",
                    iconSelected = Icons.Filled.Settings,
                    iconUnselected = Icons.Outlined.Settings,
                    onClick = { onTabSelected("settings") },
                    testTag = "tab_settings"
                )
            }
            
            // Bottom Safe Area Spacer Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun NavigationTabItem(
    selected: Boolean,
    label: String,
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val activeColor = Color(0xFF6366F1)
    val inactiveColor = Color.White.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) iconSelected else iconUnselected,
            contentDescription = label,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (selected) activeColor else inactiveColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
