package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DeviceTelemetry
import com.example.model.PermissionStatusState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse

@Composable
fun PermissionsHubScreen(
    permissionState: PermissionStatusState,
    telemetry: DeviceTelemetry,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onNotificationPermissionResult: (Boolean) -> Unit,
    onToggleAutoBridge: (Boolean) -> Unit,
    onToggleAutonomousExec: (Boolean) -> Unit,
    onToggleVoiceFeedback: (Boolean) -> Unit,
    onRefreshStatus: () -> Unit,
    onInjectFullAccessHandshake: () -> Unit
) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotificationPermissionResult(granted)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .testTag("permissions_hub_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Full-Access Readiness Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ObsidianElevated, ObsidianSurface)
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(listOf(CyanElectric, VioletPulse)),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(CyanElectric.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "درع صلاحيات الوكيل",
                                    tint = CyanElectric,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "مركز صلاحيات التحكم الكامل (Full Access)",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "مستوى جاهزية وكيل Z.ai للتحكم بالهاتف: ${permissionState.readinessScore}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyanElectric
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshStatus,
                            modifier = Modifier.testTag("refresh_permissions_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث حالة الصلاحيات",
                                tint = CyanElectric
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { permissionState.readinessScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = if (permissionState.readinessScore >= 80) EmeraldActive else CyanElectric,
                        trackColor = ObsidianDeep
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onInjectFullAccessHandshake,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_zai_agent_handshake_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanElectric,
                            contentColor = ObsidianDeep
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ربط وحقن صلاحية التحكم الكامل داخل محادثة Z.ai الآن",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Core System Permissions Cards
        item {
            Text(
                text = "1. صلاحيات النظام الأساسية للوكيل الذكي",
                style = MaterialTheme.typography.titleMedium,
                color = CyanElectric,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 1. Accessibility Service Card
        item {
            PermissionActionCard(
                title = "خدمة إمكانية الوصول والتحكم التلقائي (Accessibility Service)",
                description = "الصلاحية الأهم لتمكين وكيل Z.ai من النقر التلقائي، التمرير، قراءة نصوص الشاشة، وتنفيذ أوامر الرجوع والشاشة الرئيسية.",
                icon = Icons.Default.AccessibilityNew,
                isGranted = permissionState.isAccessibilityEnabled,
                buttonText = if (permissionState.isAccessibilityEnabled) "مفعّلة ونشطة" else "تفعيل الخدمة من الإعدادات",
                testTag = "perm_accessibility_button",
                onClick = onOpenAccessibilitySettings
            )
        }

        // 2. System Overlay Permission Card
        item {
            PermissionActionCard(
                title = "الظهور فوق التطبيقات الأخرى (System Overlay)",
                description = "يسمح لوكيل Z.ai بإظهار مؤشر التحكم والمراقبة الفورية أثناء تنقله بين تطبيقات هاتفك.",
                icon = Icons.Default.Layers,
                isGranted = permissionState.isOverlayGranted,
                buttonText = if (permissionState.isOverlayGranted) "ممنوحة بالكامل" else "منح صلاحية الظهور",
                testTag = "perm_overlay_button",
                onClick = onOpenOverlaySettings
            )
        }

        // 3. Write System Settings Card
        item {
            PermissionActionCard(
                title = "تعديل إعدادات النظام (Modify System Settings)",
                description = "يمكّن الوكيل من ضبط سطوع الشاشة، مستوى الصوت، زمن قفل الشاشة، وأوضاع الشبكة تلقائياً.",
                icon = Icons.Default.SettingsSuggest,
                isGranted = permissionState.isWriteSettingsGranted,
                buttonText = if (permissionState.isWriteSettingsGranted) "ممنوحة بالكامل" else "منح صلاحية الإعدادات",
                testTag = "perm_write_settings_button",
                onClick = onOpenWriteSettings
            )
        }

        // 4. Runtime Notifications Permission Card
        item {
            PermissionActionCard(
                title = "إشعارات العمليات الفورية للوكيل (Notifications)",
                description = "إرسال تنبيهات فورية عند إتمام المهام المؤتمتة أو استلام أوامر جديدة من جلسة Z.ai.",
                icon = Icons.Default.NotificationsActive,
                isGranted = permissionState.isNotificationsGranted,
                buttonText = if (permissionState.isNotificationsGranted) "مفعّلة" else "تفعيل الإشعارات",
                testTag = "perm_notifications_button",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onNotificationPermissionResult(true)
                    }
                }
            )
        }

        // Autonomous Agent Behavior Switches
        item {
            Text(
                text = "2. إعدادات استقلالية الوكيل (Autonomous Engine)",
                style = MaterialTheme.typography.titleMedium,
                color = CyanElectric,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianGlass, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AgentToggleRow(
                        icon = Icons.Default.Terminal,
                        title = "حقن الجسر البرمجي التلقائي (window.ZaiPhoneAgent)",
                        subtitle = "ربط صفحة Z.ai تلقائياً بواجهة برمجة الهاتف فور تحميل الجلسة",
                        checked = permissionState.isAutoBridgeEnabled,
                        testTag = "switch_auto_bridge",
                        onCheckedChange = onToggleAutoBridge
                    )
                    AgentToggleRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "التنفيذ الفوري لأوامر JSON الصادرة من Z.ai",
                        subtitle = "إضافة أزرار تنفيذ فوري تحت الأكواد البرمجية التي يولدها نموذج Z.ai",
                        checked = permissionState.isAutonomousExecutionEnabled,
                        testTag = "switch_autonomous_exec",
                        onCheckedChange = onToggleAutonomousExec
                    )
                    AgentToggleRow(
                        icon = Icons.Default.RecordVoiceOver,
                        title = "التفاعل الصوتي العربي للوكيل (Arabic TTS)",
                        subtitle = "السماح لوكيل Z.ai بالتحدث صوتياً عبر مكبر صوت الهاتف عند تنفيذ المهام",
                        checked = permissionState.isVoiceFeedbackEnabled,
                        testTag = "switch_voice_feedback",
                        onCheckedChange = onToggleVoiceFeedback
                    )
                }
            }
        }

        // Live Device Telemetry Snapshot
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "بيانات الهاتف المتصلة بجلسة Z.ai الآن",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanElectric
                    )
                    Text(
                        text = "الطراز: ${telemetry.deviceModel} | النظام: ${telemetry.androidVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "أبعاد الشاشة: ${telemetry.screenResolution} | البطارية: ${telemetry.batteryPercent}% ${if (telemetry.isCharging) "(متصل بالشاحن ⚡)" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "النافذة النشطة: ${telemetry.activeWindowPackage}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldActive
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    buttonText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isGranted) EmeraldActive.copy(alpha = 0.5f) else ObsidianGlass,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isGranted) EmeraldActive.copy(alpha = 0.15f)
                            else CyanElectric.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isGranted) EmeraldActive else CyanElectric,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (isGranted) EmeraldActive else AmberWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGranted) "الصلاحية مفعّلة بالكامل" else "تتطلب التفعيل لمنح التحكم الكامل",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGranted) EmeraldActive else AmberWarning
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) ObsidianElevated else CyanElectric,
                    contentColor = if (isGranted) EmeraldActive else ObsidianDeep
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun AgentToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanElectric,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianDeep,
                checkedTrackColor = CyanElectric
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
