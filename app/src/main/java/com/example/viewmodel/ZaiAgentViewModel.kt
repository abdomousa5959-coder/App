package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AgentCommandType
import com.example.model.AgentLogEntry
import com.example.model.AppScreen
import com.example.model.ChatMessageItem
import com.example.model.DeviceTelemetry
import com.example.model.DocumentType
import com.example.model.InstalledAppItem
import com.example.model.PermissionStatusState
import com.example.model.QuickMacro
import com.example.model.WorkspaceDocument
import com.example.service.ZaiAccessibilityService
import com.example.service.ZaiDeviceController
import com.example.service.ZaiDocumentEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "zai_agent_prefs")

class ZaiAgentViewModel(application: Application) : AndroidViewModel(application) {

    private val KEY_LAST_URL = stringPreferencesKey("last_zai_url")
    private val KEY_AUTO_BRIDGE = booleanPreferencesKey("auto_bridge_enabled")
    private val KEY_AUTONOMOUS_EXEC = booleanPreferencesKey("autonomous_exec_enabled")
    private val KEY_VOICE_FEEDBACK = booleanPreferencesKey("voice_feedback_enabled")

    private val _currentScreen = MutableStateFlow(AppScreen.AGENT_CHAT)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _sessionUrl = MutableStateFlow("https://chat.z.ai/")
    val sessionUrl: StateFlow<String> = _sessionUrl.asStateFlow()

    private val _isPageLoading = MutableStateFlow(true)
    val isPageLoading: StateFlow<Boolean> = _isPageLoading.asStateFlow()

    private val _pageLoadProgress = MutableStateFlow(0)
    val pageLoadProgress: StateFlow<Int> = _pageLoadProgress.asStateFlow()

    private val _isSessionAuthenticated = MutableStateFlow(false)
    val isSessionAuthenticated: StateFlow<Boolean> = _isSessionAuthenticated.asStateFlow()

    private val _cookieStatusSummary = MutableStateFlow("جاهز لحفظ الجلسة التلقائية (Persistent Cookies)")
    val cookieStatusSummary: StateFlow<String> = _cookieStatusSummary.asStateFlow()

    private val _permissionState = MutableStateFlow(PermissionStatusState())
    val permissionState: StateFlow<PermissionStatusState> = _permissionState.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _workspaceDocuments = MutableStateFlow<List<WorkspaceDocument>>(emptyList())
    val workspaceDocuments: StateFlow<List<WorkspaceDocument>> = _workspaceDocuments.asStateFlow()

    private val _selectedDocumentForEdit = MutableStateFlow<WorkspaceDocument?>(null)
    val selectedDocumentForEdit: StateFlow<WorkspaceDocument?> = _selectedDocumentForEdit.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessageItem>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageItem>> = _chatMessages.asStateFlow()

    private val _telemetry = MutableStateFlow(
        DeviceTelemetry(
            batteryPercent = 90,
            isCharging = true,
            screenResolution = "1080x2400",
            androidVersion = "Android 15",
            deviceModel = "ANDROID DEVICE",
            activeWindowPackage = "com.example",
            uiNodeCount = 18
        )
    )
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val _logs = MutableStateFlow<List<AgentLogEntry>>(emptyList())
    val logs: StateFlow<List<AgentLogEntry>> = _logs.asStateFlow()

    private val _pendingWebViewScript = MutableStateFlow<String?>(null)
    val pendingWebViewScript: StateFlow<String?> = _pendingWebViewScript.asStateFlow()

    private val _desktopUserAgentMode = MutableStateFlow(false)
    val desktopUserAgentMode: StateFlow<Boolean> = _desktopUserAgentMode.asStateFlow()

    private val _showFloatingQuickSheet = MutableStateFlow(false)
    val showFloatingQuickSheet: StateFlow<Boolean> = _showFloatingQuickSheet.asStateFlow()

    val documentEngine = ZaiDocumentEngine(
        context = application.applicationContext,
        onLogEvent = { type, detail, isSuccess, source ->
            appendLog(type, detail, isSuccess, source)
        }
    )

    val deviceController = ZaiDeviceController(
        context = application.applicationContext,
        documentEngine = documentEngine,
        onCommandExecuted = { type, detail, isSuccess, source ->
            appendLog(type, detail, isSuccess, source)
        },
        onDocumentCreatedOrUpdated = { doc ->
            refreshWorkspaceDocuments()
            _selectedDocumentForEdit.value = doc
        }
    )

    val quickMacros: List<QuickMacro> = listOf(
        QuickMacro(
            id = "inject_agent_prompt",
            titleAr = "تفعيل الوكيل الشامل في Z.ai",
            subtitleAr = "حقن صلاحيات التحكم بالهاتف وإنشاء ملفات PDF/Word/Excel داخل جلسة Z.ai",
            commandPromptForZai = """
                أنت الآن وكيل ذكاء اصطناعي بصلاحيات كاملة (Full Access) متصل بهاتفي الأندرويد عبر window.ZaiPhoneAgent.
                يمكنك تنفيذ أي أمر عبر إخراج كود JSON واحد ليقوم هاتفي بتنفيذه تلقائياً:
                - لإنشاء ملف PDF: {"action":"CREATE_PDF","fileName":"report.pdf","title":"العنوان","content":"المحتوى"}
                - لإنشاء ملف Word: {"action":"CREATE_WORD","fileName":"doc.docx","title":"العنوان","content":"المحتوى"}
                - لإنشاء جدول Excel: {"action":"CREATE_EXCEL","fileName":"sheet.xlsx","title":"الجدول","content":"العمود1 | العمود2\nقيمة1 | 100"}
                - لتعديل/كتابة ملف: {"action":"WRITE_FILE","fileName":"notes.txt","content":"النص الجديد"}
                - لفتح تطبيق: {"action":"LAUNCH_APP","target":"WhatsApp"}
                - للبحث على الإنترنت: {"action":"WEB_SEARCH","query":"..."}
            """.trimIndent(),
            actionCode = "INJECT_SYSTEM_PROMPT"
        ),
        QuickMacro(
            id = "send_active_file_to_zai",
            titleAr = "إرسال الملف المحدد إلى Z.ai",
            subtitleAr = "قراءة محتوى الملف الحالي وإرساله لمحادثة Z.ai لتعديله أو تلخيصه",
            commandPromptForZai = "قم بقراءة هذا الملف من هاتفي وتنفيذ التعديلات المطلوبة عليه:",
            actionCode = "SEND_FILE_TO_ZAI"
        ),
        QuickMacro(
            id = "scan_screen_to_zai",
            titleAr = "إرسال عناصر الشاشة إلى Z.ai",
            subtitleAr = "قراءة شجرة واجهة المستخدم الحالية وإرسالها للوكيل لتحليلها",
            commandPromptForZai = "قم بتحليل عناصر شاشة هاتفي الحالية واقترح الإجراء التالي:",
            actionCode = "SCAN_AND_SEND"
        ),
        QuickMacro(
            id = "quick_pdf_sample",
            titleAr = "توليد ملف PDF فوري",
            subtitleAr = "إنشاء مستند PDF تجريبي وحفظه في مساحة عمل الوكيل",
            commandPromptForZai = """{"action":"CREATE_PDF","fileName":"تقرير_الوكيل.pdf","title":"تقرير وكيل Z.ai الذكي","content":"تم توليد هذا المستند الرسمي بصيغة PDF مباشرة عبر محرك المستندات في Z.ai AgentOS."}""",
            actionCode = "EXEC_JSON"
        ),
        QuickMacro(
            id = "quick_excel_sample",
            titleAr = "توليد جدول Excel فوري",
            subtitleAr = "إنشاء ملف Excel (.xlsx) حقيقي قابل للفتح في Microsoft Excel و Google Sheets",
            commandPromptForZai = """{"action":"CREATE_EXCEL","fileName":"ميزانية_المشروع.xlsx","title":"الميزانية","content":"البند | التكلفة الشهرية | الحالة\nالخوادم السحابية | 450 | نشط\nتطوير التطبيقات | 1200 | مكتمل\nالتسويق الرقمي | 600 | جاري"}""",
            actionCode = "EXEC_JSON"
        )
    )

    init {
        appendLog(
            AgentCommandType.BRIDGE_SYNC,
            "تم تشغيل نواة Z.ai AgentOS v3.0 مع محرك قراءة وتعديل الملفات وتوليد PDF / Word / Excel",
            true,
            "System Kernel"
        )
        seedWelcomeChatMessages()
        loadPreferences()
        refreshSystemState()
        refreshWorkspaceDocuments()
        startTelemetryHeartbeat()
    }

    private fun seedWelcomeChatMessages() {
        val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        _chatMessages.value = listOf(
            ChatMessageItem(
                id = 1L,
                senderIsUser = false,
                text = "أهلاً بك! أنا وكيل **Z.ai AgentOS** الذكي المتصل بهاتفك وملفاتك بالكامل.\n\n" +
                    "تقدر تتكلم معايا هنا في الشات وتطلب مني أي حاجة فوراً، مثل:\n" +
                    "• **«اعمل ملف PDF عن خطة عمل مشروع التجارة الإلكترونية»**\n" +
                    "• **«اعمل ملف Word لتقرير المبيعات الشهري»**\n" +
                    "• **«اعمل جدول Excel لمصاريف الشهر والميزانية»**\n" +
                    "• **«اقرأ ملفاتي أو عدل على الملف المرفق»**\n" +
                    "• **«افتح يوتيوب / واتساب / إعدادات الواي فاي»** أو **«ابحث في النت عن...»**\n\n" +
                    "كما يمكنك الضغط على زر **إرفاق ملف من الهاتف** لقراءة أي ملف وتعديله مباشرة!",
                timestamp = now,
                executedActionBadge = "الوكيل جاهز للتحكم الكامل"
            )
        )
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            getApplication<Application>().dataStore.data.collect { prefs ->
                val savedUrl = prefs[KEY_LAST_URL] ?: "https://chat.z.ai/"
                if (_sessionUrl.value != savedUrl && _sessionUrl.value == "https://chat.z.ai/") {
                    _sessionUrl.value = savedUrl
                }
                val autoBridge = prefs[KEY_AUTO_BRIDGE] ?: true
                val autoExec = prefs[KEY_AUTONOMOUS_EXEC] ?: true
                val voiceFeedback = prefs[KEY_VOICE_FEEDBACK] ?: true
                _permissionState.update {
                    it.copy(
                        isAutoBridgeEnabled = autoBridge,
                        isAutonomousExecutionEnabled = autoExec,
                        isVoiceFeedbackEnabled = voiceFeedback
                    )
                }
            }
        }
    }

    private fun startTelemetryHeartbeat() {
        viewModelScope.launch {
            while (true) {
                refreshPermissionsAndTelemetryOnly()
                delay(3000L)
            }
        }
    }

    fun refreshWorkspaceDocuments() {
        val docs = documentEngine.listWorkspaceDocuments()
        _workspaceDocuments.value = docs
        if (_selectedDocumentForEdit.value == null && docs.isNotEmpty()) {
            _selectedDocumentForEdit.value = docs.first()
        }
    }

    fun selectDocumentForEditing(doc: WorkspaceDocument) {
        _selectedDocumentForEdit.value = doc
    }

    fun importExternalFileUri(uri: Uri) {
        viewModelScope.launch {
            val imported = documentEngine.importExternalUriToWorkspace(uri)
            refreshWorkspaceDocuments()
            if (imported != null) {
                _selectedDocumentForEdit.value = imported
                val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                val userMsg = ChatMessageItem(
                    id = System.nanoTime(),
                    senderIsUser = true,
                    text = "تم إرفاق الملف من الهاتف: ${imported.fileName}",
                    timestamp = now,
                    attachedDoc = imported
                )
                val agentReply = ChatMessageItem(
                    id = System.nanoTime() + 1,
                    senderIsUser = false,
                    text = "تم استيراد وقراءة الملف **${imported.fileName}** بنجاح (${imported.sizeBytes} بايت).\n\n" +
                        "محتوى الملف الحالي:\n```\n${imported.previewContent.take(600)}\n```\n" +
                        "يمكنك الآن أن تطلب مني في الشات: **«عدل الملف واكتب فيه...»** أو **«حول الملف إلى PDF أو Word أو Excel»** أو **«أرسله إلى جلسة Z.ai»**.",
                    timestamp = now,
                    attachedDoc = imported,
                    executedActionBadge = "تمت قراءة الملف"
                )
                _chatMessages.update { it + userMsg + agentReply }
            }
        }
    }

    fun saveEditedDocumentContent(
        fileName: String,
        newContent: String,
        docType: DocumentType
    ): WorkspaceDocument {
        val updated = documentEngine.updateOrSaveDocument(fileName, newContent, docType)
        refreshWorkspaceDocuments()
        _selectedDocumentForEdit.value = updated
        return updated
    }

    fun deleteWorkspaceDocument(doc: WorkspaceDocument) {
        documentEngine.deleteDocument(doc)
        refreshWorkspaceDocuments()
        if (_selectedDocumentForEdit.value?.filePath == doc.filePath) {
            _selectedDocumentForEdit.value = _workspaceDocuments.value.firstOrNull()
        }
    }

    /**
     * Handles conversational commands in the Unified Agent Chat:
     * Executes local device control, file reading/editing, PDF/Word/Excel generation,
     * web search, and optionally forwards complex reasoning to the live Z.ai session.
     */
    fun sendUserChatCommand(rawInput: String, forwardToZaiWeb: Boolean = false) {
        val prompt = rawInput.trim()
        if (prompt.isEmpty()) return

        val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val userMsg = ChatMessageItem(
            id = System.nanoTime(),
            senderIsUser = true,
            text = prompt,
            timestamp = now,
            ForwardedToZaiWeb = forwardToZaiWeb
        )
        _chatMessages.update { it + userMsg }

        viewModelScope.launch {
            val lower = prompt.lowercase()
            var createdDoc: WorkspaceDocument? = null
            var actionBadge: String? = null
            val replyText: String

            // 1. Check if raw JSON command was pasted
            if (prompt.startsWith("{") && prompt.endsWith("}") && prompt.contains("\"action\"")) {
                val ok = deviceController.executeAgentJson(prompt)
                refreshWorkspaceDocuments()
                replyText = if (ok) {
                    "تم تنفيذ أمر الـ JSON بنجاح على الهاتف."
                } else {
                    "تعذر تنفيذ أمر JSON، تأكد من الصيغة الصحيحة."
                }
                actionBadge = "تنفيذ JSON مباشر"
            }
            // 2. Check for PDF generation intent
            else if (lower.contains("pdf") || prompt.contains("بي دي اف") || prompt.contains("بى دى اف")) {
                val extractedTitle = extractTopicFromPrompt(prompt, listOf("اعمل", "انشئ", "انشاء", "ملف", "مستند", "pdf", "بي دي اف", "بى دى اف", "بعنوان", "عن"))
                    .ifBlank { "تقرير_وكيل_Zai" }
                val activeFileContent = _selectedDocumentForEdit.value?.previewContent.orEmpty()
                val body = if (lower.contains("الملف") && activeFileContent.isNotBlank()) {
                    activeFileContent
                } else {
                    buildRichDocumentContent(extractedTitle, prompt)
                }
                createdDoc = documentEngine.createPdfDocument(
                    rawFileName = extractedTitle.take(35),
                    title = extractedTitle,
                    bodyText = body
                )
                refreshWorkspaceDocuments()
                _selectedDocumentForEdit.value = createdDoc
                actionBadge = "تم إنشاء PDF"
                replyText = "تم إنشاء ملف **PDF** الرسمي بنجاح باسم: **${createdDoc.fileName}**.\n" +
                    "يمكنك فتحه أو مشاركته مباشرة من البطاقة بالأسفل، أو التعديل عليه من قسم استوديو الملفات."
            }
            // 3. Check for Word (.docx) generation intent
            else if (lower.contains("word") || lower.contains("docx") || prompt.contains("وورد") || prompt.contains("ورد")) {
                val extractedTitle = extractTopicFromPrompt(prompt, listOf("اعمل", "انشئ", "انشاء", "ملف", "مستند", "word", "docx", "وورد", "ورد", "بعنوان", "عن"))
                    .ifBlank { "مستند_وورد_Zai" }
                val activeFileContent = _selectedDocumentForEdit.value?.previewContent.orEmpty()
                val body = if (lower.contains("الملف") && activeFileContent.isNotBlank()) {
                    activeFileContent
                } else {
                    buildRichDocumentContent(extractedTitle, prompt)
                }
                createdDoc = documentEngine.createWordDocxDocument(
                    rawFileName = extractedTitle.take(35),
                    title = extractedTitle,
                    bodyText = body
                )
                refreshWorkspaceDocuments()
                _selectedDocumentForEdit.value = createdDoc
                actionBadge = "تم إنشاء Word (.docx)"
                replyText = "تم إنشاء مستند **Microsoft Word (.docx)** بنجاح باسم: **${createdDoc.fileName}**.\n" +
                    "الملف متوافق تماماً مع تطبيقات Word و Google Docs وجاهز للفتح والمشاركة."
            }
            // 4. Check for Excel (.xlsx) generation intent
            else if (lower.contains("excel") || lower.contains("xlsx") || prompt.contains("اكسيل") || prompt.contains("إكسيل") || prompt.contains("اكسل") || prompt.contains("جدول")) {
                val extractedTitle = extractTopicFromPrompt(prompt, listOf("اعمل", "انشئ", "انشاء", "ملف", "جدول", "excel", "xlsx", "اكسيل", "إكسيل", "اكسل", "بعنوان", "عن"))
                    .ifBlank { "جدول_بيانات_Zai" }
                val rows = buildSmartExcelRows(extractedTitle, prompt, _selectedDocumentForEdit.value?.previewContent)
                createdDoc = documentEngine.createExcelXlsxDocument(
                    rawFileName = extractedTitle.take(35),
                    sheetTitle = extractedTitle.take(25),
                    rows = rows
                )
                refreshWorkspaceDocuments()
                _selectedDocumentForEdit.value = createdDoc
                actionBadge = "تم إنشاء Excel (.xlsx)"
                replyText = "تم إنشاء جدول **Microsoft Excel (.xlsx)** بنجاح باسم: **${createdDoc.fileName}** (${rows.size} صفوف).\n" +
                    "يمكنك فتح الجدول مباشرة في Excel أو Sheets أو تعديل خلاياه من استوديو الملفات."
            }
            // 5. Check for Reading / Listing user files
            else if (prompt.contains("اقرأ") || prompt.contains("اقرا") || prompt.contains("ملفاتي") || lower.contains("read file")) {
                val docs = documentEngine.listWorkspaceDocuments()
                val currentDoc = _selectedDocumentForEdit.value ?: docs.firstOrNull()
                actionBadge = "قراءة الملفات"
                replyText = if (currentDoc != null) {
                    "لديك حالياً **${docs.size} ملفات** في مساحة العمل.\n" +
                        "آخر ملف نشط هو **${currentDoc.fileName}** (${currentDoc.docType.labelAr}):\n" +
                        "```\n${currentDoc.previewContent.take(700)}\n```\n" +
                        "اكتب لي: **«عدل الملف واكتب: ...»** لتحديثه فوراً أو إرفاق ملف جديد من هاتفك."
                } else {
                    "لا توجد ملفات حالياً. اضغط على زر إرفاق ملف من الهاتف أو اطلب مني إنشاء ملف جديد."
                }
            }
            // 6. Check for Editing / Modifying the active file
            else if (prompt.contains("عدل") || prompt.contains("تعديل") || prompt.contains("اكتب في الملف") || prompt.contains("حدث الملف") || prompt.contains("اضف للملف")) {
                val targetDoc = _selectedDocumentForEdit.value ?: documentEngine.listWorkspaceDocuments().firstOrNull()
                val newText = prompt
                    .replaceFirst(Regex("^(عدل|تعديل|اكتب في الملف|حدث الملف|اضف للملف|عدل الملف واكتب|عدل الملف)\\s*[:：-]?\\s*"), "")
                    .trim()
                if (targetDoc != null && newText.isNotEmpty()) {
                    val combinedContent = if (prompt.contains("اضف")) {
                        "${targetDoc.previewContent}\n$newText"
                    } else {
                        newText
                    }
                    createdDoc = documentEngine.updateOrSaveDocument(
                        fileName = targetDoc.fileName,
                        newContent = combinedContent,
                        docType = targetDoc.docType
                    )
                    refreshWorkspaceDocuments()
                    _selectedDocumentForEdit.value = createdDoc
                    actionBadge = "تم تعديل وحفظ الملف"
                    replyText = "تم تعديل وحفظ الملف **${createdDoc.fileName}** بنجاح!\n" +
                        "المحتوى الجديد:\n```\n${createdDoc.previewContent.take(500)}\n```"
                } else {
                    replyText = "لتعديل ملف، حدد الملف أولاً أو اكتب: «عدل الملف واكتب: النص الجديد هنا»."
                }
            }
            // 7. Check for Phone Control (Launch App, Settings, Navigation, Swipe, Tap)
            else if (prompt.contains("افتح") || lower.contains("open") || lower.contains("launch")) {
                val target = extractTopicFromPrompt(prompt, listOf("افتح", "شغل", "تطبيق", "برنامج", "open", "launch"))
                when {
                    target.contains("واي فاي") || target.contains("wifi", ignoreCase = true) -> {
                        deviceController.openSystemSettingPage("WIFI")
                        actionBadge = "إعدادات Wi-Fi"
                        replyText = "تم فتح إعدادات الشبكة اللاسلكية (Wi-Fi) على هاتفك."
                    }
                    target.contains("بلوتوث") || target.contains("bluetooth", ignoreCase = true) -> {
                        deviceController.openSystemSettingPage("BLUETOOTH")
                        actionBadge = "إعدادات البلوتوث"
                        replyText = "تم فتح إعدادات البلوتوث على هاتفك."
                    }
                    target.contains("إعدادات") || target.contains("اعدادات") || target.contains("settings", ignoreCase = true) -> {
                        deviceController.openSystemSettingPage("SETTINGS")
                        actionBadge = "إعدادات النظام"
                        replyText = "تم فتح إعدادات النظام للهاتف."
                    }
                    target.contains("شاشة رئيسية") || target.contains("الرئيسية") || target.contains("home", ignoreCase = true) -> {
                        deviceController.performSystemAction("HOME")
                        actionBadge = "الشاشة الرئيسية"
                        replyText = "تم الانتقال إلى الشاشة الرئيسية للهاتف."
                    }
                    else -> {
                        val launched = deviceController.launchApp(target)
                        if (launched) {
                            actionBadge = "فتح تطبيق: $target"
                            replyText = "تم تشغيل تطبيق **$target** على هاتفك بنجاح."
                        } else {
                            deviceController.searchWebOrOpenUrl(target)
                            actionBadge = "فتح عبر الويب: $target"
                            replyText = "لم يتم العثور على تطبيق مثبت باسم «$target»، لذا قمت بفتحه مباشرة عبر متصفح الإنترنت."
                        }
                    }
                }
            }
            // 8. Check for Web Search / Internet task
            else if (prompt.contains("ابحث") || prompt.contains("بحث") || prompt.contains("نت") || prompt.contains("انترنت") || prompt.contains("إنترنت") || lower.contains("search") || lower.contains("http")) {
                val query = extractTopicFromPrompt(prompt, listOf("ابحث", "بحث", "في", "علي", "على", "النت", "الانترنت", "الإنترنت", "عن", "جوجل", "search"))
                    .ifBlank { prompt }
                deviceController.searchWebOrOpenUrl(query)
                actionBadge = "بحث على الإنترنت"
                replyText = "تم تنفيذ البحث وفتح النتائج على الإنترنت لـ: **$query**.\n" +
                    "كما يمكنك إرسال الطلب إلى جلسة Z.ai المباشرة لتحليل النتائج بعمق."
            }
            // 9. Check for Screen Reading / UI hierarchy
            else if (prompt.contains("شاشة") || prompt.contains("الشاشة") || prompt.contains("مرر") || prompt.contains("انقر")) {
                if (prompt.contains("مرر")) {
                    deviceController.swipe(540f, 1600f, 540f, 420f, 320L)
                    actionBadge = "تمرير الشاشة"
                    replyText = "تم تنفيذ إيماءة التمرير التلقائي على الشاشة."
                } else {
                    val summary = deviceController.readScreenHierarchy()
                    actionBadge = "قراءة الشاشة الحية"
                    replyText = "تم فحص عناصر الشاشة الحالية بنجاح:\n```\n$summary\n```"
                }
            }
            // 10. General instruction -> Execute & optionally bridge to live Z.ai session
            else {
                actionBadge = "معالجة أمر الوكيل"
                replyText = "تم استلام أمرك: **«$prompt»**.\n" +
                    "• إذا كنت تريد حفظه كمستند، اختر من الأزرار السريعة بالأسفل (**تحويل إلى PDF** / **Word** / **Excel**).\n" +
                    "• أو اضغط على **«إرسال وتنفيذ في جلسة Z.ai الحية»** ليقوم نموذج Z.ai الكامل بمعالجته وإصدار أوامر التحكم بهاتفك."
            }

            if (forwardToZaiWeb) {
                injectPromptIntoZaiSession(prompt)
            }

            if (_permissionState.value.isVoiceFeedbackEnabled && actionBadge != null) {
                deviceController.speak(actionBadge)
            }

            val agentMsg = ChatMessageItem(
                id = System.nanoTime(),
                senderIsUser = false,
                text = replyText,
                timestamp = SimpleDateFormat("HH:mm", Locale.US).format(Date()),
                attachedDoc = createdDoc,
                executedActionBadge = actionBadge,
                ForwardedToZaiWeb = forwardToZaiWeb
            )
            _chatMessages.update { it + agentMsg }
        }
    }

    private fun extractTopicFromPrompt(prompt: String, stopWords: List<String>): String {
        var result = prompt
        stopWords.forEach { word ->
            result = result.replace(Regex("(?i)\\b$word\\b"), "")
        }
        return result.replace(Regex("[:：\"'«»]"), "").trim().ifBlank { "مستند_وكيل_Zai" }
    }

    private fun buildRichDocumentContent(title: String, originalPrompt: String): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        return """
            عنوان المستند: $title
            تاريخ الإنشاء: $dateStr
            بواسطة: وكيل الذكاء الاصطناعي Z.ai AgentOS
            --------------------------------------------------
            
            1. ملخص تنفيذي:
            تم إعداد هذا المستند تلقائياً بناءً على طلب المستخدم: "$originalPrompt".
            يهدف هذا التقرير إلى تنظيم النقاط الأساسية وتقديم خطة عمل واضحة وقابلة للتنفيذ.
            
            2. المحاور والنقاط الرئيسية:
            • التحليل الأولي وتحديد المتطلبات الأساسية للموضوع ($title).
            • خطوات التنفيذ العملي وتوزيع المهام الزمنية بكفاءة عالية.
            • مؤشرات قياس الأداء والجودة لضمان تحقيق أفضل النتائج.
            
            3. التوصيات والخطوات القادمة:
            • مراجعة البيانات المرفقة وتحديثها دورياً عبر محرر الملفات في Z.ai AgentOS.
            • مشاركة المستند مع فريق العمل أو تصديره بصيغ PDF و Word و Excel.
        """.trimIndent()
    }

    private fun buildSmartExcelRows(
        title: String,
        prompt: String,
        activePreview: String?
    ): List<List<String>> {
        if (!activePreview.isNullOrBlank() && (prompt.contains("الملف") || activePreview.contains("|") || activePreview.contains(","))) {
            return documentEngine.parseTextToTableRows(activePreview)
        }
        if (prompt.contains("|") || prompt.lines().size > 2) {
            return documentEngine.parseTextToTableRows(prompt)
        }
        return listOf(
            listOf("م", "البند / المهمة ($title)", "التكلفة / القيمة", "النسبة", "الحالة"),
            listOf("1", "التخطيط وإعداد المتطلبات", "1500", "100%", "مكتمل"),
            listOf("2", "التنفيذ والتشغيل الأساسي", "3200", "85%", "قيد التنفيذ"),
            listOf("3", "المراجعة وضبط الجودة", "950", "60%", "نشط"),
            listOf("4", "الإطلاق النهائي والمتابعة", "1100", "40%", "مجدول"),
            listOf("الإجمالي", "مجموع عناصر $title", "6750", "71%", "معتمد")
        )
    }

    fun refreshSystemState() {
        viewModelScope.launch {
            refreshPermissionsAndTelemetryOnly()
            val apps = deviceController.getInstalledApps()
            _installedApps.value = apps
            refreshWorkspaceDocuments()
        }
    }

    fun refreshPermissionsAndTelemetryOnly() {
        val accEnabled = deviceController.isAccessibilityServiceEnabled()
        val overlayEnabled = deviceController.isOverlayPermissionGranted()
        val writeEnabled = deviceController.isWriteSettingsGranted()

        _permissionState.update {
            it.copy(
                isAccessibilityEnabled = accEnabled,
                isOverlayGranted = overlayEnabled,
                isWriteSettingsGranted = writeEnabled
            )
        }
        _telemetry.value = deviceController.getDeviceTelemetry()
    }

    fun updateNotificationPermissionStatus(granted: Boolean) {
        _permissionState.update { it.copy(isNotificationsGranted = granted) }
    }

    fun selectScreen(screen: AppScreen) {
        _currentScreen.value = screen
        refreshPermissionsAndTelemetryOnly()
    }

    fun toggleFloatingQuickSheet(show: Boolean) {
        _showFloatingQuickSheet.value = show
    }

    fun loadTargetUrl(url: String) {
        val formatted = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
        _sessionUrl.value = formatted
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[KEY_LAST_URL] = formatted
            }
        }
    }

    fun onPageLoadStateChanged(loading: Boolean, progress: Int) {
        _isPageLoading.value = loading
        _pageLoadProgress.value = progress
    }

    fun onCookiesInspected(cookieHeader: String?) {
        val hasCookies = !cookieHeader.isNullOrBlank()
        _isSessionAuthenticated.value = hasCookies &&
            (cookieHeader.contains("token", ignoreCase = true) ||
                cookieHeader.contains("session", ignoreCase = true) ||
                cookieHeader.contains("auth", ignoreCase = true) ||
                cookieHeader.length > 40)

        _cookieStatusSummary.value = if (_isSessionAuthenticated.value) {
            "جلسة Z.ai موثقة ونشطة (${cookieHeader?.length ?: 0} بايت من ملفات الارتباط المشفرة)"
        } else if (hasCookies) {
            "الجلسة متصلة - ملفات تعريف الارتباط محفوظة تلقائياً"
        } else {
            "يرجى تسجيل الدخول في Z.ai لحفظ الجلسة الدائمة"
        }
    }

    fun setAutoBridgeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[KEY_AUTO_BRIDGE] = enabled }
            _permissionState.update { it.copy(isAutoBridgeEnabled = enabled) }
        }
    }

    fun setAutonomousExecutionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[KEY_AUTONOMOUS_EXEC] = enabled }
            _permissionState.update { it.copy(isAutonomousExecutionEnabled = enabled) }
        }
    }

    fun setVoiceFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[KEY_VOICE_FEEDBACK] = enabled }
            _permissionState.update { it.copy(isVoiceFeedbackEnabled = enabled) }
        }
    }

    fun toggleDesktopMode() {
        _desktopUserAgentMode.update { !it }
        appendLog(
            AgentCommandType.BRIDGE_SYNC,
            if (_desktopUserAgentMode.value) "تم التبديل إلى وضع سطح المكتب (Desktop UI)" else "تم التبديل إلى وضع الهاتف المحمول (Mobile UI)",
            true,
            "Session Browser"
        )
    }

    /**
     * Injects a prompt or Full-Access Agent Bridge handshake directly into the live Z.ai chat input box
     * and attempts to click the Send button automatically.
     */
    fun injectPromptIntoZaiSession(promptText: String) {
        val escaped = promptText
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

        val script = """
            (function() {
                try {
                    var textarea = document.querySelector('textarea') || 
                                   document.querySelector('[contenteditable="true"]') ||
                                   document.querySelector('input[type="text"]');
                    if (textarea) {
                        textarea.focus();
                        if (textarea.tagName.toLowerCase() === 'textarea' || textarea.tagName.toLowerCase() === 'input') {
                            var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, "value")?.set ||
                                                         Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value")?.set;
                            if (nativeInputValueSetter) {
                                nativeInputValueSetter.call(textarea, "$escaped");
                            } else {
                                textarea.value = "$escaped";
                            }
                            textarea.dispatchEvent(new Event('input', { bubbles: true }));
                        } else {
                            textarea.innerText = "$escaped";
                            textarea.dispatchEvent(new Event('input', { bubbles: true }));
                        }
                        return "INJECTED_OK";
                    } else {
                        return "NO_INPUT_FOUND";
                    }
                } catch(e) {
                    return "ERR:" + e.message;
                }
            })();
        """.trimIndent()

        _pendingWebViewScript.value = script
        _currentScreen.value = AppScreen.SESSION
        appendLog(
            AgentCommandType.BRIDGE_SYNC,
            "تم حقن أمر الوكيل مباشرة في حقل محادثة Z.ai: \"${promptText.take(48)}...\"",
            true,
            "Z.ai Bridge"
        )
    }

    fun consumePendingWebViewScript() {
        _pendingWebViewScript.value = null
    }

    fun executeQuickMacro(macro: QuickMacro) {
        when (macro.actionCode) {
            "INJECT_SYSTEM_PROMPT" -> {
                val contextJson = deviceController.getDeviceContextJson()
                val fullHandshake = """
                    ${macro.commandPromptForZai}
                    بيانات الهاتف والملفات الحالية:
                    $contextJson
                """.trimIndent()
                injectPromptIntoZaiSession(fullHandshake)
            }
            "SEND_FILE_TO_ZAI" -> {
                val doc = _selectedDocumentForEdit.value ?: _workspaceDocuments.value.firstOrNull()
                if (doc != null) {
                    val payload = """
                        ${macro.commandPromptForZai}
                        اسم الملف: ${doc.fileName} (${doc.docType.labelAr})
                        محتوى الملف:
                        ${doc.previewContent}
                        
                        إذا قمت بتعديل الملف أو تحويله، أخرج كود JSON بهذا الشكل ليقوم هاتفي بحفظه تلقائياً:
                        {"action":"WRITE_FILE","fileName":"${doc.fileName}","content":"المحتوى المعدل هنا"}
                    """.trimIndent()
                    injectPromptIntoZaiSession(payload)
                }
            }
            "SCAN_AND_SEND" -> {
                val uiSummary = deviceController.readScreenHierarchy()
                injectPromptIntoZaiSession("${macro.commandPromptForZai}\n$uiSummary")
            }
            else -> {
                deviceController.executeAgentJson(macro.commandPromptForZai)
                refreshWorkspaceDocuments()
            }
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        appendLog(
            AgentCommandType.BRIDGE_SYNC,
            "تم تصفير سجل العمليات وإعادة المراقبة الفورية",
            true,
            "System Kernel"
        )
    }

    fun appendLog(
        type: AgentCommandType,
        detail: String,
        isSuccess: Boolean,
        source: String = "Z.ai Bridge"
    ) {
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val entry = AgentLogEntry(
            id = System.nanoTime(),
            timestamp = timeFmt,
            commandType = type,
            detail = detail,
            isSuccess = isSuccess,
            source = source
        )
        _logs.update { current -> (listOf(entry) + current).take(120) }
    }

    override fun onCleared() {
        super.onCleared()
        deviceController.shutdown()
    }
}
