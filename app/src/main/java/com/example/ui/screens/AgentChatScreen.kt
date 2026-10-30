package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessageItem
import com.example.model.DocumentType
import com.example.model.WorkspaceDocument
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
fun AgentChatScreen(
    messages: List<ChatMessageItem>,
    activeDocument: WorkspaceDocument?,
    onSendCommand: (String, Boolean) -> Unit,
    onImportFileUri: (Uri) -> Unit,
    onOpenDocument: (WorkspaceDocument) -> Unit,
    onShareDocument: (WorkspaceDocument) -> Unit,
    onEditDocumentInStudio: (WorkspaceDocument) -> Unit,
    onSwitchToZaiWebSession: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var autoForwardToZaiWeb by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportFileUri(uri)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .testTag("agent_chat_screen")
    ) {
        // Top Agent Status & Active File Header
        Surface(
            color = ObsidianSurface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(CyanElectric, VioletPulse))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = ObsidianDeep,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "شات وكيل Z.ai الشامل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "يتحكم بالهاتف • يقرأ ويعدل ملفاتك • ينشئ PDF و Word و Excel",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanElectric
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onSwitchToZaiWebSession,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_open_zai_web_from_chat")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = CyanElectric,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جلسة Z.ai", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Active File Context Banner
                if (activeDocument != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianElevated)
                            .border(1.dp, CyanElectric.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { onEditDocumentInStudio(activeDocument) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = when (activeDocument.docType) {
                                    DocumentType.PDF -> Icons.Default.PictureAsPdf
                                    DocumentType.EXCEL -> Icons.Default.TableChart
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = CyanElectric,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الملف النشط للوكيل: ${activeDocument.fileName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "فتح المحرر ✎",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldActive
                        )
                    }
                }
            }
        }

        // Chat Messages Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubbleCard(
                    message = msg,
                    onOpenDocument = onOpenDocument,
                    onShareDocument = onShareDocument,
                    onEditDocumentInStudio = onEditDocumentInStudio
                )
            }
        }

        // Bottom Action Chips + Chat Input Bar
        Surface(
            color = ObsidianSurface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Quick Action Prompts Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickPrompts = listOf(
                        "📄 اعمل ملف PDF عن خطة العمل" to "اعمل ملف PDF عن خطة العمل الشهرية وتطوير المشروع",
                        "📝 اعمل ملف Word تقرير" to "اعمل ملف Word عن تقرير الأداء والمهام المنجزة",
                        "📊 اعمل جدول Excel للميزانية" to "اعمل جدول Excel لميزانية المشروع والمصاريف الشهرية",
                        "📂 اقرأ ملفاتي الحالية" to "اقرأ ملفاتي الحالية واعرض محتواها",
                        "🌐 ابحث في النت عن أخبار الذكاء الاصطناعي" to "ابحث في النت عن أحدث تطورات الذكاء الاصطناعي",
                        "📱 افتح إعدادات الواي فاي" to "افتح إعدادات الواي فاي"
                    )
                    quickPrompts.forEachIndexed { idx, (label, fullCmd) ->
                        ElevatedFilterChip(
                            selected = false,
                            onClick = { onSendCommand(fullCmd, autoForwardToZaiWeb) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                containerColor = ObsidianElevated,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("chat_quick_chip_$idx")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Toggle to simultaneously forward command to live Z.ai Web session
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إرسال تلقائي أيضاً إلى جلسة Z.ai الحية عند الضغط على إرسال",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Switch(
                        checked = autoForwardToZaiWeb,
                        onCheckedChange = { autoForwardToZaiWeb = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianDeep,
                            checkedTrackColor = CyanElectric
                        ),
                        modifier = Modifier.testTag("switch_forward_to_zai_web")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Input Row with File Picker + Text Field + Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianElevated)
                            .border(1.dp, CyanElectric.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .testTag("btn_attach_file_from_device")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "إرفاق ملف من الهاتف لقراءته وتعديله",
                            tint = CyanElectric
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_command_input"),
                        placeholder = {
                            Text("اطلب أي شيء: اعمل PDF / Word / Excel، عدل ملف، افتح تطبيق...")
                        },
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    onSendCommand(inputText, autoForwardToZaiWeb)
                                    inputText = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanElectric,
                            unfocusedBorderColor = ObsidianGlass
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendCommand(inputText, autoForwardToZaiWeb)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyanElectric)
                            .testTag("btn_send_chat_command")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال الأمر للوكيل",
                            tint = ObsidianDeep
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleCard(
    message: ChatMessageItem,
    onOpenDocument: (WorkspaceDocument) -> Unit,
    onShareDocument: (WorkspaceDocument) -> Unit,
    onEditDocumentInStudio: (WorkspaceDocument) -> Unit
) {
    val isUser = message.senderIsUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) CyanElectric.copy(alpha = 0.16f) else ObsidianSurface
            ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 4.dp else 18.dp,
                bottomEnd = if (isUser) 18.dp else 4.dp
            ),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .border(
                    width = 1.dp,
                    color = if (isUser) CyanElectric.copy(alpha = 0.45f) else ObsidianGlass,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 4.dp else 18.dp,
                        bottomEnd = if (isUser) 18.dp else 4.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "أنت" else "وكيل Z.ai الذكي",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isUser) CyanElectric else EmeraldActive
                    )
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                if (message.executedActionBadge != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldActive.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldActive,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = message.executedActionBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldActive
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Attached or Generated Document Card inside Chat Bubble
                val doc = message.attachedDoc
                if (doc != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianElevated)
                            .border(1.dp, CyanElectric.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (doc.docType) {
                                        DocumentType.PDF -> Icons.Default.PictureAsPdf
                                        DocumentType.EXCEL -> Icons.Default.TableChart
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = when (doc.docType) {
                                        DocumentType.PDF -> AmberWarning
                                        DocumentType.EXCEL -> EmeraldActive
                                        else -> CyanElectric
                                    },
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.fileName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${doc.docType.labelAr} • ${doc.sizeBytes} بايت",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { onOpenDocument(doc) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanElectric,
                                        contentColor = ObsidianDeep
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("فتح", style = MaterialTheme.typography.labelLarge)
                                }

                                FilledTonalButton(
                                    onClick = { onEditDocumentInStudio(doc) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعديل", style = MaterialTheme.typography.labelLarge)
                                }

                                FilledTonalButton(
                                    onClick = { onShareDocument(doc) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
