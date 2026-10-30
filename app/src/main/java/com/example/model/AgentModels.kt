package com.example.model

enum class AppScreen {
    AGENT_CHAT,
    SESSION,
    FILES_STUDIO,
    PERMISSIONS,
    CONSOLE,
    LOGS
}

enum class AgentCommandType(val labelAr: String, val codeName: String) {
    LAUNCH_APP("فتح تطبيق", "LAUNCH_APP"),
    TAP("نقر إحداثي", "TAP_XY"),
    SWIPE("تمرير الشاشة", "SWIPE_GESTURE"),
    GLOBAL_HOME("الشاشة الرئيسية", "GLOBAL_HOME"),
    GLOBAL_BACK("رجوع للخلف", "GLOBAL_BACK"),
    GLOBAL_RECENTS("التطبيقات الأخيرة", "GLOBAL_RECENTS"),
    GLOBAL_NOTIFICATIONS("لوحة الإشعارات", "GLOBAL_NOTIFICATIONS"),
    READ_SCREEN("قراءة محتوى الشاشة", "DUMP_UI_TREE"),
    CLIPBOARD_SET("نسخ للحافظة", "SET_CLIPBOARD"),
    SPEAK_TTS("نطق صوتي للوكيل", "SPEAK_TTS"),
    SYSTEM_SETTING("فتح إعدادات النظام", "OPEN_SETTINGS"),
    BRIDGE_SYNC("حقن الجسر في Z.ai", "INJECT_ZAI_BRIDGE"),
    FILE_READ("قراءة ملف", "READ_FILE"),
    FILE_WRITE("تعديل/حفظ ملف", "WRITE_FILE"),
    CREATE_PDF("إنشاء مستند PDF", "CREATE_PDF"),
    CREATE_WORD("إنشاء مستند Word", "CREATE_DOCX"),
    CREATE_EXCEL("إنشاء جدول Excel", "CREATE_XLSX"),
    WEB_AUTOMATION("بحث وتصفح الإنترنت", "WEB_TASK")
}

enum class DocumentType(val labelAr: String, val extension: String, val mimeType: String) {
    PDF("مستند PDF", "pdf", "application/pdf"),
    WORD("مستند Word", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    EXCEL("جدول Excel", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    TEXT("ملف نصي/كود", "txt", "text/plain")
}

data class WorkspaceDocument(
    val id: String,
    val fileName: String,
    val filePath: String,
    val docType: DocumentType,
    val sizeBytes: Long,
    val lastModified: String,
    val previewContent: String
)

data class ChatMessageItem(
    val id: Long,
    val senderIsUser: Boolean,
    val text: String,
    val timestamp: String,
    val attachedDoc: WorkspaceDocument? = null,
    val executedActionBadge: String? = null,
    val ForwardedToZaiWeb: Boolean = false
)

data class AgentLogEntry(
    val id: Long,
    val timestamp: String,
    val commandType: AgentCommandType,
    val detail: String,
    val isSuccess: Boolean,
    val source: String = "Z.ai Bridge"
)

data class InstalledAppItem(
    val appName: String,
    val packageName: String
)

data class DeviceTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val screenResolution: String,
    val androidVersion: String,
    val deviceModel: String,
    val activeWindowPackage: String,
    val uiNodeCount: Int
)

data class PermissionStatusState(
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isWriteSettingsGranted: Boolean = false,
    val isNotificationsGranted: Boolean = false,
    val isAutoBridgeEnabled: Boolean = true,
    val isAutonomousExecutionEnabled: Boolean = true,
    val isVoiceFeedbackEnabled: Boolean = true
) {
    val readinessScore: Int
        get() {
            var score = 20 // Base WebView + JS Bridge + File Engine active
            if (isAccessibilityEnabled) score += 35
            if (isOverlayGranted) score += 15
            if (isWriteSettingsGranted) score += 15
            if (isNotificationsGranted) score += 15
            return score.coerceAtMost(100)
        }
}

data class QuickMacro(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val commandPromptForZai: String,
    val actionCode: String
)
