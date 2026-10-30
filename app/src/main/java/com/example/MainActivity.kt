package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppScreen
import com.example.service.ZaiAccessibilityService
import com.example.ui.screens.ActivityLogScreen
import com.example.ui.screens.AgentChatScreen
import com.example.ui.screens.AgentCommandCenterScreen
import com.example.ui.screens.FileDocumentStudioScreen
import com.example.ui.screens.PermissionsHubScreen
import com.example.ui.screens.ZaiSessionScreen
import com.example.ui.theme.CyanDim
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse
import com.example.viewmodel.ZaiAgentViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ZaiAgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ZaiAgentOsRoot(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemState()
    }
}

private data class NavDestination(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZaiAgentOsRoot(viewModel: ZaiAgentViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val sessionUrl by viewModel.sessionUrl.collectAsStateWithLifecycle()
    val isLoading by viewModel.isPageLoading.collectAsStateWithLifecycle()
    val loadProgress by viewModel.pageLoadProgress.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isSessionAuthenticated.collectAsStateWithLifecycle()
    val cookieSummary by viewModel.cookieStatusSummary.collectAsStateWithLifecycle()
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val workspaceDocuments by viewModel.workspaceDocuments.collectAsStateWithLifecycle()
    val selectedDocument by viewModel.selectedDocumentForEdit.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val pendingScript by viewModel.pendingWebViewScript.collectAsStateWithLifecycle()
    val desktopMode by viewModel.desktopUserAgentMode.collectAsStateWithLifecycle()
    val showQuickSheet by viewModel.showFloatingQuickSheet.collectAsStateWithLifecycle()
    val screenNodes by ZaiAccessibilityService.screenTreeSnapshot.collectAsStateWithLifecycle()

    if (currentScreen != AppScreen.AGENT_CHAT) {
        BackHandler {
            viewModel.selectScreen(AppScreen.AGENT_CHAT)
        }
    }

    val destinations = listOf(
        NavDestination(AppScreen.AGENT_CHAT, "شات الوكيل", Icons.Default.Chat, "nav_tab_agent_chat"),
        NavDestination(AppScreen.FILES_STUDIO, "ملفاتي", Icons.Default.FolderCopy, "nav_tab_files_studio"),
        NavDestination(AppScreen.SESSION, "جلسة Z.ai", Icons.Default.Language, "nav_tab_session"),
        NavDestination(AppScreen.CONSOLE, "القيادة", Icons.Default.Terminal, "nav_tab_console"),
        NavDestination(AppScreen.PERMISSIONS, "الصلاحيات", Icons.Default.Security, "nav_tab_permissions")
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianDeep,
            floatingActionButton = {
                if (currentScreen != AppScreen.AGENT_CHAT) {
                    FloatingActionButton(
                        onClick = { viewModel.toggleFloatingQuickSheet(true) },
                        containerColor = CyanElectric,
                        contentColor = ObsidianDeep,
                        shape = CircleShape,
                        modifier = Modifier.testTag("fab_agent_quick_dock")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "لوحة أوامر وكيل Z.ai السريعة"
                        )
                    }
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 8.dp
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentScreen == dest.screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectScreen(dest.screen) },
                                icon = {
                                    if (dest.screen == AppScreen.PERMISSIONS) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = if (permissionState.isAccessibilityEnabled) EmeraldActive else VioletPulse
                                                ) {
                                                    Text("${permissionState.readinessScore}%")
                                                }
                                            }
                                        ) {
                                            Icon(dest.icon, contentDescription = dest.label)
                                        }
                                    } else if (dest.screen == AppScreen.FILES_STUDIO && workspaceDocuments.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = CyanElectric) {
                                                    Text("${workspaceDocuments.size}")
                                                }
                                            }
                                        ) {
                                            Icon(dest.icon, contentDescription = dest.label)
                                        }
                                    } else {
                                        Icon(dest.icon, contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanElectric,
                                    selectedTextColor = CyanElectric,
                                    indicatorColor = CyanDim,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        destinations.forEach { dest ->
                            val selected = currentScreen == dest.screen
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectScreen(dest.screen) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyanElectric,
                                    selectedTextColor = CyanElectric,
                                    indicatorColor = CyanDim,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentScreen) {
                        AppScreen.AGENT_CHAT -> AgentChatScreen(
                            messages = chatMessages,
                            activeDocument = selectedDocument,
                            onSendCommand = { cmd, forward -> viewModel.sendUserChatCommand(cmd, forward) },
                            onImportFileUri = { uri -> viewModel.importExternalFileUri(uri) },
                            onOpenDocument = { doc -> viewModel.documentEngine.openDocumentExternally(doc) },
                            onShareDocument = { doc -> viewModel.documentEngine.shareDocument(doc) },
                            onEditDocumentInStudio = { doc ->
                                viewModel.selectDocumentForEditing(doc)
                                viewModel.selectScreen(AppScreen.FILES_STUDIO)
                            },
                            onSwitchToZaiWebSession = { viewModel.selectScreen(AppScreen.SESSION) }
                        )

                        AppScreen.FILES_STUDIO -> FileDocumentStudioScreen(
                            documents = workspaceDocuments,
                            selectedDocument = selectedDocument,
                            onSelectDocument = { viewModel.selectDocumentForEditing(it) },
                            onImportExternalFileUri = { viewModel.importExternalFileUri(it) },
                            onSaveOrCreateDocument = { name, content, type ->
                                viewModel.saveEditedDocumentContent(name, content, type)
                            },
                            onDeleteDocument = { viewModel.deleteWorkspaceDocument(it) },
                            onOpenExternally = { viewModel.documentEngine.openDocumentExternally(it) },
                            onShareDocument = { viewModel.documentEngine.shareDocument(it) },
                            onSendDocumentToZaiSession = { doc ->
                                viewModel.selectDocumentForEditing(doc)
                                viewModel.executeQuickMacro(viewModel.quickMacros[1])
                            }
                        )

                        AppScreen.SESSION -> ZaiSessionScreen(
                            sessionUrl = sessionUrl,
                            isLoading = isLoading,
                            loadProgress = loadProgress,
                            isAuthenticated = isAuthenticated,
                            cookieSummary = cookieSummary,
                            permissionState = permissionState,
                            desktopMode = desktopMode,
                            pendingScript = pendingScript,
                            quickMacros = viewModel.quickMacros,
                            deviceController = viewModel.deviceController,
                            onUrlSubmitted = { viewModel.loadTargetUrl(it) },
                            onPageLoadStateChanged = { l, p -> viewModel.onPageLoadStateChanged(l, p) },
                            onCookiesInspected = { viewModel.onCookiesInspected(it) },
                            onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                            onScriptConsumed = { viewModel.consumePendingWebViewScript() },
                            onExecuteMacro = { viewModel.executeQuickMacro(it) },
                            onOpenPermissionsTab = { viewModel.selectScreen(AppScreen.PERMISSIONS) }
                        )

                        AppScreen.PERMISSIONS -> PermissionsHubScreen(
                            permissionState = permissionState,
                            telemetry = telemetry,
                            onOpenAccessibilitySettings = { viewModel.deviceController.openAccessibilitySettings() },
                            onOpenOverlaySettings = { viewModel.deviceController.openOverlaySettings() },
                            onOpenWriteSettings = { viewModel.deviceController.openWriteSettingsPermission() },
                            onNotificationPermissionResult = { viewModel.updateNotificationPermissionStatus(it) },
                            onToggleAutoBridge = { viewModel.setAutoBridgeEnabled(it) },
                            onToggleAutonomousExec = { viewModel.setAutonomousExecutionEnabled(it) },
                            onToggleVoiceFeedback = { viewModel.setVoiceFeedbackEnabled(it) },
                            onRefreshStatus = { viewModel.refreshSystemState() },
                            onInjectFullAccessHandshake = {
                                viewModel.executeQuickMacro(viewModel.quickMacros.first())
                            }
                        )

                        AppScreen.CONSOLE -> AgentCommandCenterScreen(
                            installedApps = installedApps,
                            screenNodes = screenNodes,
                            deviceController = viewModel.deviceController,
                            onSendScreenContextToZai = {
                                viewModel.executeQuickMacro(viewModel.quickMacros[2])
                            }
                        )

                        AppScreen.LOGS -> ActivityLogScreen(
                            logs = logs,
                            onClearLogs = { viewModel.clearLogs() }
                        )
                    }
                }
            }
        }

        // Quick Floating Agent Control Sheet (accessible from anywhere)
        if (showQuickSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleFloatingQuickSheet(false) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = ObsidianSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = CyanElectric,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "مركز الأوامر الفورية لوكيل Z.ai",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تحكم مباشر بالهاتف وتوليد مستندات PDF/Word/Excel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.toggleFloatingQuickSheet(false)
                                viewModel.selectScreen(AppScreen.LOGS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianElevated,
                                contentColor = CyanElectric
                            )
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("السجل (${logs.size})")
                        }
                    }

                    viewModel.quickMacros.forEach { macro ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianElevated)
                                .border(1.dp, ObsidianGlass, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = macro.titleAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = macro.subtitleAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    viewModel.toggleFloatingQuickSheet(false)
                                    viewModel.executeQuickMacro(macro)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanElectric,
                                    contentColor = ObsidianDeep
                                ),
                                modifier = Modifier.testTag("sheet_macro_${macro.id}")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تنفيذ")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
