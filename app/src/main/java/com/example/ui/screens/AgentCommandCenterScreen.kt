package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.InstalledAppItem
import com.example.service.ZaiAccessibilityService
import com.example.service.ZaiDeviceController
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentCommandCenterScreen(
    installedApps: List<InstalledAppItem>,
    screenNodes: List<String>,
    deviceController: ZaiDeviceController,
    onSendScreenContextToZai: () -> Unit
) {
    var tapX by remember { mutableStateOf("540") }
    var tapY by remember { mutableStateOf("1100") }
    var targetClickText by remember { mutableStateOf("") }
    var ttsInputText by remember { mutableStateOf("مرحباً، أنا وكيل زد إيه آي وأتحكم بالهاتف الآن") }
    var jsonCommandInput by remember { mutableStateOf("""{"action":"SYSTEM","target":"HOME"}""") }
    var appSearchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(installedApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(appSearchQuery, ignoreCase = true) ||
                it.packageName.contains(appSearchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .testTag("agent_command_center_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Global Hardware & Navigation Actions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianGlass, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "لوحة التحكم المباشر بالنظام (Global System Actions)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanElectric
                    )
                    Text(
                        text = "اختبر ونفّذ الأوامر التي يستخدمها وكيل Z.ai للتنقل في الهاتف",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SystemControlChip(
                            label = "الرئيسية (Home)",
                            icon = Icons.Default.Home,
                            tag = "cmd_global_home"
                        ) { deviceController.performSystemAction("HOME") }

                        SystemControlChip(
                            label = "رجوع (Back)",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            tag = "cmd_global_back"
                        ) { deviceController.performSystemAction("BACK") }

                        SystemControlChip(
                            label = "التطبيقات الأخيرة",
                            icon = Icons.Default.ViewCarousel,
                            tag = "cmd_global_recents"
                        ) { deviceController.performSystemAction("RECENTS") }

                        SystemControlChip(
                            label = "الإشعارات",
                            icon = Icons.Default.Notifications,
                            tag = "cmd_global_notifications"
                        ) { deviceController.performSystemAction("NOTIFICATIONS") }

                        SystemControlChip(
                            label = "الواي فاي",
                            icon = Icons.Default.Wifi,
                            tag = "cmd_open_wifi"
                        ) { deviceController.openSystemSettingPage("WIFI") }

                        SystemControlChip(
                            label = "البلوتوث",
                            icon = Icons.Default.Bluetooth,
                            tag = "cmd_open_bluetooth"
                        ) { deviceController.openSystemSettingPage("BLUETOOTH") }
                    }
                }
            }
        }

        // Section 2: Installed Applications Launcher (Real PackageManager query)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianGlass, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = CyanElectric
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تشغيل تطبيقات الهاتف (${installedApps.size} تطبيق متاح للوكيل)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_installed_apps_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = CyanElectric)
                        },
                        label = { Text("ابحث عن تطبيق لفتحه عبر الوكيل...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanElectric,
                            unfocusedBorderColor = ObsidianGlass
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredApps.isEmpty()) {
                        Text(
                            text = "لا توجد تطبيقات مطابقة للبحث الحالي.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredApps.take(30), key = { it.packageName }) { appItem ->
                                Box(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(ObsidianElevated)
                                        .border(1.dp, CyanElectric.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                        .clickable { deviceController.launchApp(appItem.packageName) }
                                        .padding(12.dp)
                                        .testTag("launch_app_${appItem.packageName}")
                                ) {
                                    Column {
                                        Text(
                                            text = appItem.appName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = appItem.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "تشغيل فوري ⚡",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyanElectric
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Gesture & Coordinate Tap / Text Click Controller
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianGlass, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyanElectric)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "محرك الإيماءات والنقر الذكي (Touch & UI Node Actuation)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tapX,
                            onValueChange = { tapX = it },
                            label = { Text("الإحداثي X") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_tap_x")
                        )
                        OutlinedTextField(
                            value = tapY,
                            onValueChange = { tapY = it },
                            label = { Text("الإحداثي Y") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_tap_y")
                        )
                        Button(
                            onClick = {
                                val x = tapX.toFloatOrNull() ?: 540f
                                val y = tapY.toFloatOrNull() ?: 1100f
                                deviceController.tap(x, y)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanElectric,
                                contentColor = ObsidianDeep
                            ),
                            modifier = Modifier.testTag("execute_tap_xy_button")
                        ) {
                            Text("نقر (Tap)")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { deviceController.swipe(540f, 1600f, 540f, 400f, 320L) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_swipe_up")
                        ) {
                            Icon(Icons.Default.SwipeVertical, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تمرير لأعلى")
                        }
                        FilledTonalButton(
                            onClick = { deviceController.swipe(540f, 450f, 540f, 1600f, 320L) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_swipe_down")
                        ) {
                            Icon(Icons.Default.SwipeVertical, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تمرير لأسفل")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = targetClickText,
                            onValueChange = { targetClickText = it },
                            label = { Text("النقر على زر يحتوي على نص...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_click_by_text")
                        )
                        Button(
                            onClick = {
                                if (targetClickText.isNotBlank()) {
                                    deviceController.clickByText(targetClickText)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VioletPulse,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("btn_click_by_text")
                        ) {
                            Text("نقر بالنص")
                        }
                    }
                }
            }
        }

        // Section 4: Live Screen Hierarchy Inspector & Z.ai Vision Bridge
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianGlass, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = EmeraldActive)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "قارئ عناصر الشاشة الحية (UI Tree Inspector)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianDeep)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "الحزمة النشطة: ${ZaiAccessibilityService.activePackage.value}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanElectric
                            )
                            screenNodes.take(8).forEach { nodeLine ->
                                Text(
                                    text = "• $nodeLine",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onSendScreenContextToZai,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_send_screen_to_zai"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanElectric,
                            contentColor = ObsidianDeep
                        )
                    ) {
                        Text(
                            text = "إرسال قراءة الشاشة الحالية مباشرة إلى محادثة Z.ai",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Section 5: Voice Synthesis & Direct JSON Command Executor
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianGlass, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = CyanElectric)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "النطق الصوتي وتنفيذ أوامر JSON المباشرة",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ttsInputText,
                            onValueChange = { ttsInputText = it },
                            label = { Text("رسالة صوتية ينطقها الوكيل") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_tts_text")
                        )
                        Button(
                            onClick = { deviceController.speak(ttsInputText) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanElectric,
                                contentColor = ObsidianDeep
                            ),
                            modifier = Modifier.testTag("btn_speak_tts")
                        ) {
                            Text("نطق")
                        }
                    }

                    OutlinedTextField(
                        value = jsonCommandInput,
                        onValueChange = { jsonCommandInput = it },
                        label = { Text("أمر JSON للوكيل (مثال: {\"action\":\"LAUNCH_APP\",\"target\":\"Settings\"})") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_raw_json_command")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { deviceController.executeAgentJson(jsonCommandInput) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_execute_json"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldActive,
                                contentColor = ObsidianDeep
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تنفيذ أمر JSON")
                        }

                        FilledTonalButton(
                            onClick = { deviceController.copyToClipboard(jsonCommandInput) },
                            modifier = Modifier.testTag("btn_copy_json")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemControlChip(
    label: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianElevated)
            .border(1.dp, CyanElectric.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = CyanElectric,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
