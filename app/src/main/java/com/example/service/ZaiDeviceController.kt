package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.DisplayMetrics
import android.view.WindowManager
import android.webkit.JavascriptInterface
import com.example.model.AgentCommandType
import com.example.model.DeviceTelemetry
import com.example.model.DocumentType
import com.example.model.InstalledAppItem
import com.example.model.WorkspaceDocument
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.Locale

/**
 * Real Android Device Controller, File & Document Bridge (`window.ZaiPhoneAgent`)
 * injected into the Z.ai WebView session and exposed to the Unified Agent Chat.
 *
 * Capabilities:
 * - Launch installed apps by name or package
 * - Dispatch coordinate taps, swipes, or click screen elements by text
 * - Perform global navigation (Home, Back, Recents, Notifications)
 * - Read, edit, and save local files (TXT, JSON, CSV, XML, DOCX, XLSX)
 * - Generate real PDF, Microsoft Word (.docx), and Microsoft Excel (.xlsx) documents on demand
 * - Search the web or open URLs directly
 * - Control clipboard, haptic vibration, system settings, and Arabic/English TTS voice output
 */
class ZaiDeviceController(
    private val context: Context,
    val documentEngine: ZaiDocumentEngine,
    private val onCommandExecuted: (AgentCommandType, String, Boolean, String) -> Unit,
    private val onDocumentCreatedOrUpdated: (WorkspaceDocument) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ar"))
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                if (!isTtsReady) {
                    tts?.language = Locale.US
                    isTtsReady = true
                }
            }
        }
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        if (ZaiAccessibilityService.isConnected.value) return true
        val expectedComponentName = "${context.packageName}/${ZaiAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabledServices.contains(expectedComponentName, ignoreCase = true)
    }

    fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isWriteSettingsGranted(): Boolean {
        return Settings.System.canWrite(context)
    }

    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onCommandExecuted(
                AgentCommandType.SYSTEM_SETTING,
                "فتح إعدادات إمكانية الوصول لتفعيل وكيل Z.ai",
                true,
                "Permissions Hub"
            )
        } catch (e: Exception) {
            onCommandExecuted(
                AgentCommandType.SYSTEM_SETTING,
                "تعذر فتح إعدادات إمكانية الوصول: ${e.message}",
                false,
                "Permissions Hub"
            )
        }
    }

    fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onCommandExecuted(
                AgentCommandType.SYSTEM_SETTING,
                "فتح صلاحية الظهور فوق التطبيقات الأخرى (Overlay)",
                true,
                "Permissions Hub"
            )
        } catch (e: Exception) {
            onCommandExecuted(
                AgentCommandType.SYSTEM_SETTING,
                "تعذر فتح إعدادات الظهور فوق التطبيقات",
                false,
                "Permissions Hub"
            )
        }
    }

    fun openWriteSettingsPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onCommandExecuted(
                AgentCommandType.SYSTEM_SETTING,
                "فتح صلاحية تعديل إعدادات النظام (السطوع والصوت)",
                true,
                "Permissions Hub"
            )
        } catch (e: Exception) {
            onCommandExecuted(
                AgentCommandType.SYSTEM_SETTING,
                "تعذر فتح إعدادات تعديل النظام",
                false,
                "Permissions Hub"
            )
        }
    }

    fun getInstalledApps(): List<InstalledAppItem> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }
        return resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                if (label.isEmpty()) return@mapNotNull null
                InstalledAppItem(appName = label, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun getDeviceTelemetry(): DeviceTelemetry {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 85

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        return DeviceTelemetry(
            batteryPercent = pct,
            isCharging = isCharging,
            screenResolution = "${metrics.widthPixels}x${metrics.heightPixels}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
            activeWindowPackage = ZaiAccessibilityService.activePackage.value,
            uiNodeCount = ZaiAccessibilityService.nodeCount.value
        )
    }

    fun triggerHapticPulse(durationMs: Long = 45L) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }

    @JavascriptInterface
    fun launchApp(target: String): Boolean {
        triggerHapticPulse()
        val cleanTarget = target.trim()
        val pm = context.packageManager

        val directIntent = pm.getLaunchIntentForPackage(cleanTarget)
        if (directIntent != null) {
            directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(directIntent)
            onCommandExecuted(
                AgentCommandType.LAUNCH_APP,
                "تم تشغيل الحزمة: $cleanTarget",
                true,
                "Z.ai Agent"
            )
            return true
        }

        val match = getInstalledApps().firstOrNull {
            it.appName.contains(cleanTarget, ignoreCase = true) ||
                it.packageName.contains(cleanTarget, ignoreCase = true)
        }
        if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                onCommandExecuted(
                    AgentCommandType.LAUNCH_APP,
                    "تم فتح التطبيق: ${match.appName} (${match.packageName})",
                    true,
                    "Z.ai Agent"
                )
                return true
            }
        }

        onCommandExecuted(
            AgentCommandType.LAUNCH_APP,
            "لم يتم العثور على التطبيق المطلوب: $cleanTarget",
            false,
            "Z.ai Agent"
        )
        return false
    }

    @JavascriptInterface
    fun searchWebOrOpenUrl(queryOrUrl: String): Boolean {
        triggerHapticPulse(35L)
        return try {
            val trimmed = queryOrUrl.trim()
            val targetUri = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                Uri.parse(trimmed)
            } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
                Uri.parse("https://$trimmed")
            } else {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                Uri.parse("https://www.google.com/search?q=$encoded")
            }
            val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onCommandExecuted(
                AgentCommandType.WEB_AUTOMATION,
                "تنفيذ بحث/فتح على الإنترنت: $trimmed",
                true,
                "Z.ai Web Agent"
            )
            true
        } catch (e: Exception) {
            onCommandExecuted(
                AgentCommandType.WEB_AUTOMATION,
                "تعذر فتح الرابط أو البحث: ${e.message}",
                false,
                "Z.ai Web Agent"
            )
            false
        }
    }

    @JavascriptInterface
    fun createPdf(fileName: String, title: String, content: String): String {
        triggerHapticPulse(45L)
        val doc = documentEngine.createPdfDocument(
            rawFileName = fileName.ifBlank { title.ifBlank { "تقرير_PDF" } },
            title = title.ifBlank { fileName },
            bodyText = content
        )
        onDocumentCreatedOrUpdated(doc)
        return doc.filePath
    }

    @JavascriptInterface
    fun createWord(fileName: String, title: String, content: String): String {
        triggerHapticPulse(45L)
        val doc = documentEngine.createWordDocxDocument(
            rawFileName = fileName.ifBlank { title.ifBlank { "مستند_Word" } },
            title = title.ifBlank { fileName },
            bodyText = content
        )
        onDocumentCreatedOrUpdated(doc)
        return doc.filePath
    }

    @JavascriptInterface
    fun createExcel(fileName: String, sheetTitle: String, tableText: String): String {
        triggerHapticPulse(45L)
        val rows = documentEngine.parseTextToTableRows(tableText)
        val doc = documentEngine.createExcelXlsxDocument(
            rawFileName = fileName.ifBlank { sheetTitle.ifBlank { "جدول_Excel" } },
            sheetTitle = sheetTitle.ifBlank { "بيانات" },
            rows = rows
        )
        onDocumentCreatedOrUpdated(doc)
        return doc.filePath
    }

    @JavascriptInterface
    fun readFile(fileName: String): String {
        val docs = documentEngine.listWorkspaceDocuments()
        val match = docs.firstOrNull {
            it.fileName.equals(fileName.trim(), ignoreCase = true) ||
                it.fileName.contains(fileName.trim(), ignoreCase = true)
        }
        return if (match != null) {
            onCommandExecuted(
                AgentCommandType.FILE_READ,
                "تم قراءة محتوى الملف: ${match.fileName}",
                true,
                "Z.ai File Bridge"
            )
            match.previewContent
        } else {
            "لم يتم العثور على الملف: $fileName"
        }
    }

    @JavascriptInterface
    fun writeFile(fileName: String, content: String): Boolean {
        triggerHapticPulse(35L)
        val ext = File(fileName).extension.lowercase()
        val docType = when (ext) {
            "pdf" -> DocumentType.PDF
            "docx", "doc" -> DocumentType.WORD
            "xlsx", "xls" -> DocumentType.EXCEL
            else -> DocumentType.TEXT
        }
        val doc = documentEngine.updateOrSaveDocument(fileName, content, docType)
        onDocumentCreatedOrUpdated(doc)
        return true
    }

    @JavascriptInterface
    fun listFilesJson(): String {
        val docs = documentEngine.listWorkspaceDocuments()
        val arr = JSONArray()
        docs.forEach { d ->
            arr.put(
                JSONObject().apply {
                    put("fileName", d.fileName)
                    put("type", d.docType.name)
                    put("sizeBytes", d.sizeBytes)
                    put("lastModified", d.lastModified)
                }
            )
        }
        return arr.toString()
    }

    @JavascriptInterface
    fun tap(x: Float, y: Float): Boolean {
        triggerHapticPulse(30L)
        val service = ZaiAccessibilityService.instance.value
        return if (service != null) {
            val ok = service.performTap(x, y)
            onCommandExecuted(
                AgentCommandType.TAP,
                "نقر تلقائي عند الإحداثيات (${x.toInt()}, ${y.toInt()})",
                ok,
                "Z.ai Agent"
            )
            ok
        } else {
            onCommandExecuted(
                AgentCommandType.TAP,
                "محاكاة نقر (${x.toInt()}, ${y.toInt()}) - يرجى تفعيل خدمة إمكانية الوصول للتنفيذ خارج التطبيق",
                true,
                "Z.ai Sandbox"
            )
            true
        }
    }

    @JavascriptInterface
    fun clickByText(text: String): Boolean {
        triggerHapticPulse(35L)
        val service = ZaiAccessibilityService.instance.value
        return if (service != null) {
            val ok = service.clickNodeByText(text)
            onCommandExecuted(
                AgentCommandType.TAP,
                "البحث والنقر على العنصر النصي: \"$text\"",
                ok,
                "Z.ai Agent"
            )
            ok
        } else {
            onCommandExecuted(
                AgentCommandType.TAP,
                "أمر نقر على العنصر \"$text\" (يتطلب تفعيل إمكانية الوصول للتحكم الخارجي)",
                false,
                "Z.ai Agent"
            )
            false
        }
    }

    @JavascriptInterface
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 320L): Boolean {
        triggerHapticPulse(40L)
        val service = ZaiAccessibilityService.instance.value
        return if (service != null) {
            val ok = service.performSwipe(startX, startY, endX, endY, durationMs)
            onCommandExecuted(
                AgentCommandType.SWIPE,
                "تمرير من (${startX.toInt()},${startY.toInt()}) إلى (${endX.toInt()},${endY.toInt()})",
                ok,
                "Z.ai Agent"
            )
            ok
        } else {
            onCommandExecuted(
                AgentCommandType.SWIPE,
                "أمر تمرير مسجل (${startX.toInt()},${startY.toInt()} -> ${endX.toInt()},${endY.toInt()})",
                true,
                "Z.ai Sandbox"
            )
            true
        }
    }

    @JavascriptInterface
    fun performSystemAction(actionName: String): Boolean {
        triggerHapticPulse(50L)
        val service = ZaiAccessibilityService.instance.value
        val normalized = actionName.uppercase().trim()

        val (actionConst, cmdType, desc) = when (normalized) {
            "HOME" -> Triple(AccessibilityService.GLOBAL_ACTION_HOME, AgentCommandType.GLOBAL_HOME, "الانتقال إلى الشاشة الرئيسية للهاتف")
            "BACK" -> Triple(AccessibilityService.GLOBAL_ACTION_BACK, AgentCommandType.GLOBAL_BACK, "تنفيذ أمر الرجوع للخلف (Back)")
            "RECENTS" -> Triple(AccessibilityService.GLOBAL_ACTION_RECENTS, AgentCommandType.GLOBAL_RECENTS, "فتح قائمة التطبيقات الأخيرة (Recents)")
            "NOTIFICATIONS" -> Triple(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, AgentCommandType.GLOBAL_NOTIFICATIONS, "سحب شريط الإشعارات العلوي")
            else -> Triple(AccessibilityService.GLOBAL_ACTION_HOME, AgentCommandType.GLOBAL_HOME, "تنفيذ إجراء نظام: $normalized")
        }

        return if (service != null) {
            val ok = service.performGlobalNavigation(actionConst)
            onCommandExecuted(cmdType, desc, ok, "Z.ai Agent")
            ok
        } else if (normalized == "HOME") {
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                onCommandExecuted(cmdType, "$desc (عبر نظام Android Intent)", true, "Z.ai Agent")
                true
            } catch (e: Exception) {
                onCommandExecuted(cmdType, "يتطلب تفعيل إمكانية الوصول لتنفيذ: $desc", false, "Z.ai Agent")
                false
            }
        } else {
            onCommandExecuted(cmdType, "تم إرسال الأمر ($desc) - فعّل إمكانية الوصول للتنفيذ على مستوى النظام", false, "Z.ai Agent")
            false
        }
    }

    @JavascriptInterface
    fun copyToClipboard(text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Z.ai Agent", text))
            triggerHapticPulse(30L)
            onCommandExecuted(
                AgentCommandType.CLIPBOARD_SET,
                "تم نسخ النص إلى حافظة الهاتف: \"${text.take(45)}\"",
                true,
                "Z.ai Agent"
            )
            true
        } catch (e: Exception) {
            onCommandExecuted(
                AgentCommandType.CLIPBOARD_SET,
                "فشل النسخ إلى الحافظة: ${e.message}",
                false,
                "Z.ai Agent"
            )
            false
        }
    }

    @JavascriptInterface
    fun speak(text: String): Boolean {
        triggerHapticPulse(25L)
        return if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zai_agent_utt")
            onCommandExecuted(
                AgentCommandType.SPEAK_TTS,
                "نطق صوتي للوكيل: \"${text.take(50)}\"",
                true,
                "Z.ai Voice"
            )
            true
        } else {
            onCommandExecuted(
                AgentCommandType.SPEAK_TTS,
                "محرك النطق الصوتي قيد التهيئة: \"$text\"",
                false,
                "Z.ai Voice"
            )
            false
        }
    }

    @JavascriptInterface
    fun openSystemSettingPage(settingType: String): Boolean {
        triggerHapticPulse(40L)
        val action = when (settingType.uppercase().trim()) {
            "WIFI" -> Settings.ACTION_WIFI_SETTINGS
            "BLUETOOTH" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "DISPLAY" -> Settings.ACTION_DISPLAY_SETTINGS
            "SOUND" -> Settings.ACTION_SOUND_SETTINGS
            "BATTERY" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "ACCESSIBILITY" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            onCommandExecuted(
                AgentCommandType.SYSTEM_SETTING,
                "فتح قسم الإعدادات: $settingType",
                true,
                "Z.ai Agent"
            )
            true
        } catch (e: Exception) {
            onCommandExecuted(
                AgentCommandType.SYSTEM_SETTING,
                "تعذر فتح إعدادات $settingType",
                false,
                "Z.ai Agent"
            )
            false
        }
    }

    @JavascriptInterface
    fun readScreenHierarchy(): String {
        val service = ZaiAccessibilityService.instance.value
        val summary = service?.captureCurrentUiSummary()
            ?: "Package=${ZaiAccessibilityService.activePackage.value} | Tree=${ZaiAccessibilityService.screenTreeSnapshot.value.joinToString(" | ")}"
        onCommandExecuted(
            AgentCommandType.READ_SCREEN,
            "قراءة عناصر الشاشة النشطة (${ZaiAccessibilityService.nodeCount.value} عنصر)",
            true,
            "Z.ai Vision"
        )
        return summary
    }

    @JavascriptInterface
    fun getDeviceContextJson(): String {
        val telemetry = getDeviceTelemetry()
        val apps = getInstalledApps().take(20).joinToString(", ") { "${it.appName} (${it.packageName})" }
        val files = documentEngine.listWorkspaceDocuments().take(15).joinToString(", ") { "${it.fileName} (${it.docType.labelAr})" }
        return JSONObject().apply {
            put("agent", "Z.ai Phone & File Controller v3.0")
            put("deviceModel", telemetry.deviceModel)
            put("androidVersion", telemetry.androidVersion)
            put("batteryPercent", telemetry.batteryPercent)
            put("isCharging", telemetry.isCharging)
            put("screenResolution", telemetry.screenResolution)
            put("accessibilityActive", isAccessibilityServiceEnabled())
            put("overlayActive", isOverlayPermissionGranted())
            put("writeSettingsActive", isWriteSettingsGranted())
            put("activeWindowPackage", telemetry.activeWindowPackage)
            put("workspaceFiles", files)
            put("installedAppsSample", apps)
        }.toString()
    }

    /**
     * Parses structured JSON commands emitted inside Z.ai chat responses or from the local Agent Chat:
     * - {"action": "CREATE_PDF", "fileName": "report.pdf", "title": "تقرير", "content": "..."}
     * - {"action": "CREATE_WORD", "fileName": "doc.docx", "title": "مستند", "content": "..."}
     * - {"action": "CREATE_EXCEL", "fileName": "sheet.xlsx", "title": "ميزانية", "content": "البند | التكلفة\nالخادم | 250"}
     * - {"action": "WRITE_FILE", "fileName": "notes.txt", "content": "..."}
     * - {"action": "WEB_SEARCH", "query": "..."}
     * - {"action": "LAUNCH_APP", "target": "WhatsApp"}
     * - {"action": "TAP", "x": 540, "y": 1100}
     * - {"action": "SWIPE", "x1": 500, "y1": 1500, "x2": 500, "y2": 400}
     * - {"action": "SYSTEM", "target": "HOME"}
     */
    @JavascriptInterface
    fun executeAgentJson(jsonPayload: String): Boolean {
        return try {
            val obj = JSONObject(jsonPayload)
            val action = obj.optString("action", "").uppercase()
            when (action) {
                "CREATE_PDF", "PDF" -> {
                    createPdf(
                        fileName = obj.optString("fileName", "تقرير_وكيل_Zai.pdf"),
                        title = obj.optString("title", "مستند PDF من وكيل Z.ai"),
                        content = obj.optString("content", obj.optString("text", ""))
                    )
                    true
                }
                "CREATE_WORD", "WORD", "DOCX" -> {
                    createWord(
                        fileName = obj.optString("fileName", "مستند_وكيل_Zai.docx"),
                        title = obj.optString("title", "مستند Word من وكيل Z.ai"),
                        content = obj.optString("content", obj.optString("text", ""))
                    )
                    true
                }
                "CREATE_EXCEL", "EXCEL", "XLSX" -> {
                    createExcel(
                        fileName = obj.optString("fileName", "جدول_وكيل_Zai.xlsx"),
                        sheetTitle = obj.optString("title", "جدول بيانات"),
                        tableText = obj.optString("content", obj.optString("rows", "البند | القيمة"))
                    )
                    true
                }
                "WRITE_FILE", "EDIT_FILE", "SAVE_FILE" -> {
                    writeFile(
                        fileName = obj.optString("fileName", "ملف_معدل.txt"),
                        content = obj.optString("content", "")
                    )
                }
                "READ_FILE" -> {
                    readFile(obj.optString("fileName", ""))
                    true
                }
                "WEB_SEARCH", "SEARCH", "OPEN_URL" -> {
                    searchWebOrOpenUrl(obj.optString("query", obj.optString("url", obj.optString("target", ""))))
                }
                "LAUNCH_APP", "OPEN_APP" -> launchApp(obj.optString("target", ""))
                "TAP", "CLICK" -> {
                    if (obj.has("text")) {
                        clickByText(obj.optString("text"))
                    } else {
                        tap(
                            obj.optDouble("x", 540.0).toFloat(),
                            obj.optDouble("y", 960.0).toFloat()
                        )
                    }
                }
                "SWIPE", "SCROLL" -> swipe(
                    obj.optDouble("x1", 540.0).toFloat(),
                    obj.optDouble("y1", 1450.0).toFloat(),
                    obj.optDouble("x2", 540.0).toFloat(),
                    obj.optDouble("y2", 450.0).toFloat(),
                    obj.optLong("duration", 320L)
                )
                "SYSTEM", "GLOBAL" -> performSystemAction(obj.optString("target", "HOME"))
                "SPEAK", "TTS" -> speak(obj.optString("text", "تم تنفيذ الأمر عبر وكيل زد إيه آي"))
                "CLIPBOARD", "COPY" -> copyToClipboard(obj.optString("text", ""))
                "SETTINGS" -> openSystemSettingPage(obj.optString("target", "WIFI"))
                "READ_SCREEN" -> {
                    readScreenHierarchy()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            onCommandExecuted(
                AgentCommandType.BRIDGE_SYNC,
                "خطأ في تحليل أمر JSON من الوكيل: ${e.message}",
                false,
                "JSON Parser"
            )
            false
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
