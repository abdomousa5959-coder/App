package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DocumentType
import com.example.model.WorkspaceDocument
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse

@Composable
fun FileDocumentStudioScreen(
    documents: List<WorkspaceDocument>,
    selectedDocument: WorkspaceDocument?,
    onSelectDocument: (WorkspaceDocument) -> Unit,
    onImportExternalFileUri: (Uri) -> Unit,
    onSaveOrCreateDocument: (String, String, DocumentType) -> WorkspaceDocument,
    onDeleteDocument: (WorkspaceDocument) -> Unit,
    onOpenExternally: (WorkspaceDocument) -> Unit,
    onShareDocument: (WorkspaceDocument) -> Unit,
    onSendDocumentToZaiSession: (WorkspaceDocument) -> Unit
) {
    var editorFileName by remember { mutableStateOf("تقرير_جديد.pdf") }
    var editorContent by remember {
        mutableStateOf(
            "خطة العمل الشهرية\n\n1. تحليل متطلبات المشروع وتحديد الأولويات.\n2. أتمتة المهام اليومية عبر وكيل Z.ai.\n3. مراجعة مؤشرات الأداء."
        )
    }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedDocument) {
        if (selectedDocument != null) {
            editorFileName = selectedDocument.fileName
            editorContent = selectedDocument.previewContent
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportExternalFileUri(uri)
            statusBanner = "تم استيراد الملف من الهاتف بنجاح وفتحه في المحرر!"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .testTag("file_document_studio_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Header & Import From Phone Storage
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanElectric.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "استوديو الملفات والمستندات (PDF / Word / Excel)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "افتح أي ملف من هاتفك لقراءته وتعديله، أو أنشئ ملفات PDF و Word و Excel بضغطة واحدة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { openDocLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_studio_import_file"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanElectric,
                                contentColor = ObsidianDeep
                            )
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح ملف من الهاتف")
                        }

                        FilledTonalButton(
                            onClick = {
                                editorFileName = "ملف_جديد_${System.currentTimeMillis() % 1000}.txt"
                                editorContent = ""
                                statusBanner = "تم تجهيز محرر لملف جديد"
                            },
                            modifier = Modifier.testTag("btn_studio_new_blank")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ملف فارغ")
                        }
                    }

                    if (statusBanner != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✅ $statusBanner",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldActive
                        )
                    }
                }
            }
        }

        // Section 2: Horizontal List of Saved Workspace Files
        item {
            Column {
                Text(
                    text = "ملفاتك المحفوظة والمستوردة (${documents.size} ملف)",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanElectric
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(documents, key = { it.filePath }) { doc ->
                        val isSelected = selectedDocument?.filePath == doc.filePath
                        Box(
                            modifier = Modifier
                                .width(190.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) ObsidianElevated else ObsidianSurface)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CyanElectric else ObsidianGlass,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectDocument(doc) }
                                .padding(12.dp)
                                .testTag("workspace_doc_${doc.fileName}")
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = doc.docType.labelAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanElectric
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${doc.sizeBytes} بايت • ${doc.lastModified}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "فتح",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldActive,
                                        modifier = Modifier.clickable { onOpenExternally(doc) }
                                    )
                                    Text(
                                        text = "مشاركة",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CyanElectric,
                                        modifier = Modifier.clickable { onShareDocument(doc) }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف",
                                        tint = CrimsonAlert,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { onDeleteDocument(doc) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Live File Reader, Editor & Format Exporter (PDF, Word, Excel, TXT)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianGlass, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "محرر النصوص وتوليد ملفات PDF و Word و Excel",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanElectric
                    )

                    OutlinedTextField(
                        value = editorFileName,
                        onValueChange = { editorFileName = it },
                        label = { Text("اسم الملف (مثال: خطة_المشروع)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_filename_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanElectric,
                            unfocusedBorderColor = ObsidianGlass
                        )
                    )

                    OutlinedTextField(
                        value = editorContent,
                        onValueChange = { editorContent = it },
                        label = {
                            Text("محتوى الملف (لجداول Excel افصل الأعمدة بعلامة | أو فاصلة ,)")
                        },
                        minLines = 7,
                        maxLines = 15,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_content_editor"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanElectric,
                            unfocusedBorderColor = ObsidianGlass
                        )
                    )

                    Text(
                        text = "حفظ وتصدير المحتوى الحالي بصيغة:",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )

                    // Row 1: Generate PDF or Word (.docx)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val base = editorFileName.substringBeforeLast(".")
                                val saved = onSaveOrCreateDocument("$base.pdf", editorContent, DocumentType.PDF)
                                statusBanner = "تم إنشاء وحفظ ملف PDF: ${saved.fileName}"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_pdf"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberWarning,
                                contentColor = ObsidianDeep
                            )
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ كـ PDF")
                        }

                        Button(
                            onClick = {
                                val base = editorFileName.substringBeforeLast(".")
                                val saved = onSaveOrCreateDocument("$base.docx", editorContent, DocumentType.WORD)
                                statusBanner = "تم إنشاء وحفظ مستند Word: ${saved.fileName}"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_word"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanElectric,
                                contentColor = ObsidianDeep
                            )
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ كـ Word")
                        }
                    }

                    // Row 2: Generate Excel (.xlsx) or Save Text/Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val base = editorFileName.substringBeforeLast(".")
                                val saved = onSaveOrCreateDocument("$base.xlsx", editorContent, DocumentType.EXCEL)
                                statusBanner = "تم إنشاء وحفظ جدول Excel: ${saved.fileName}"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_excel"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldActive,
                                contentColor = ObsidianDeep
                            )
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ كـ Excel")
                        }

                        FilledTonalButton(
                            onClick = {
                                val saved = onSaveOrCreateDocument(editorFileName, editorContent, DocumentType.TEXT)
                                statusBanner = "تم حفظ التعديلات على: ${saved.fileName}"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_text_file")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ التعديل النصي")
                        }
                    }

                    // Row 3: Send active file to live Z.ai session or Open externally
                    if (selectedDocument != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onSendDocumentToZaiSession(selectedDocument) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_send_doc_to_zai"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VioletPulse,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال الملف لمحادثة Z.ai لتعديله")
                            }

                            IconButton(
                                onClick = { onOpenExternally(selectedDocument) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ObsidianElevated)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "فتح الملف في تطبيق خارجي",
                                    tint = CyanElectric
                                )
                            }

                            IconButton(
                                onClick = { onShareDocument(selectedDocument) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ObsidianElevated)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "مشاركة الملف",
                                    tint = EmeraldActive
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
