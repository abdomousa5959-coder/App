package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.model.AgentCommandType
import com.example.model.DocumentType
import com.example.model.WorkspaceDocument
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Real Document & File System Engine for Z.ai AgentOS:
 * - Reads user files from device storage (URI or workspace directory), including TXT, JSON, CSV, XML, DOCX, XLSX
 * - Edits and saves changes back to device files or SAF URIs
 * - Generates genuine multi-page PDF documents (`android.graphics.pdf.PdfDocument`)
 * - Generates genuine Microsoft Word `.docx` OpenXML ZIP packages
 * - Generates genuine Microsoft Excel `.xlsx` OpenXML ZIP spreadsheets
 * - Shares and opens generated files via Android `FileProvider`
 */
class ZaiDocumentEngine(
    private val context: Context,
    private val onLogEvent: (AgentCommandType, String, Boolean, String) -> Unit
) {

    val workspaceDir: File by lazy {
        File(context.filesDir, "zai_agent_workspace").apply {
            if (!exists()) mkdirs()
        }
    }

    init {
        seedInitialSampleIfEmpty()
    }

    private fun seedInitialSampleIfEmpty() {
        val existing = workspaceDir.listFiles()
        if (existing.isNullOrEmpty()) {
            createTextFile(
                rawFileName = "دليل_أوامر_وكيل_Zai.txt",
                content = """
                    مرحباً بك في مساحة ملفات وكيل Z.ai الذكي!
                    يمكنك أن تطلب من الوكيل في الشات أو داخل جلسة Z.ai:
                    1. "اعمل ملف PDF بعنوان خطة العمل الشهرية..."
                    2. "اعمل ملف Word عن تقرير المبيعات..."
                    3. "اعمل جدول Excel لميزانية المشروع..."
                    4. "افتح أي ملف من هاتفي واقرأه وعدل عليه..."
                    5. "افتح واتساب أو يوتيوب أو ابحث على الإنترنت..."
                """.trimIndent()
            )
        }
    }

    fun listWorkspaceDocuments(): List<WorkspaceDocument> {
        val files = workspaceDir.listFiles()?.sortedByDescending { it.lastModified() }.orEmpty()
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return files.map { file ->
            val ext = file.extension.lowercase()
            val docType = when (ext) {
                "pdf" -> DocumentType.PDF
                "docx", "doc" -> DocumentType.WORD
                "xlsx", "xls", "csv" -> DocumentType.EXCEL
                else -> DocumentType.TEXT
            }
            val preview = readDocumentTextPreview(file, docType)
            WorkspaceDocument(
                id = file.name,
                fileName = file.name,
                filePath = file.absolutePath,
                docType = docType,
                sizeBytes = file.length(),
                lastModified = fmt.format(Date(file.lastModified())),
                previewContent = preview
            )
        }
    }

    fun readDocumentTextPreview(file: File, docType: DocumentType): String {
        return try {
            when (docType) {
                DocumentType.TEXT -> file.readText().take(2500)
                DocumentType.WORD -> extractTextFromDocxFile(file).ifBlank { "مستند Word جاهز للفتح والمشاركة (${file.name})" }
                DocumentType.EXCEL -> extractTextFromXlsxFile(file).ifBlank { "جدول بيانات Excel جاهز للفتح والمشاركة (${file.name})" }
                DocumentType.PDF -> {
                    val sidecar = File(workspaceDir, ".${file.nameWithoutExtension}.meta.txt")
                    if (sidecar.exists()) sidecar.readText()
                    else "مستند PDF جاهز للعرض والطباعة (${file.length() / 1024} كيلوبايت)"
                }
            }
        } catch (e: Exception) {
            "ملف محفوظ في مساحة الوكيل (${file.name})"
        }
    }

    /**
     * Imports any file selected by the user via Android Storage Access Framework (`OpenDocument`)
     * into the Agent Workspace and extracts its full text so the user and Z.ai can read & edit it.
     */
    fun importExternalUriToWorkspace(uri: Uri): WorkspaceDocument? {
        return try {
            var displayName = "imported_file_${System.currentTimeMillis()}.txt"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
            val targetFile = File(workspaceDir, sanitizeFileName(displayName))
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            onLogEvent(
                AgentCommandType.FILE_READ,
                "تم استيراد وقراءة الملف من الهاتف: ${targetFile.name}",
                true,
                "File Engine"
            )
            listWorkspaceDocuments().firstOrNull { it.fileName == targetFile.name }
        } catch (e: Exception) {
            onLogEvent(
                AgentCommandType.FILE_READ,
                "فشل قراءة الملف الخارجي: ${e.message}",
                false,
                "File Engine"
            )
            null
        }
    }

    /**
     * Reads text content from an external URI directly without copying if needed.
     */
    fun readTextFromUri(uri: Uri): String {
        return try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.appendLine(line)
                        line = reader.readLine()
                    }
                }
            }
            sb.toString()
        } catch (e: Exception) {
            "تعذر قراءة النص: ${e.message}"
        }
    }

    /**
     * Saves or updates a text/code/markdown file or regenerates PDF/Word/Excel with updated content.
     */
    fun updateOrSaveDocument(
        fileName: String,
        newContent: String,
        docType: DocumentType
    ): WorkspaceDocument {
        return when (docType) {
            DocumentType.PDF -> createPdfDocument(
                rawFileName = fileName.removeSuffix(".pdf"),
                title = fileName.removeSuffix(".pdf"),
                bodyText = newContent
            )
            DocumentType.WORD -> createWordDocxDocument(
                rawFileName = fileName.removeSuffix(".docx"),
                title = fileName.removeSuffix(".docx"),
                bodyText = newContent
            )
            DocumentType.EXCEL -> {
                val rows = parseTextToTableRows(newContent)
                createExcelXlsxDocument(
                    rawFileName = fileName.removeSuffix(".xlsx"),
                    sheetTitle = "بيانات الوكيل",
                    rows = rows
                )
            }
            DocumentType.TEXT -> createTextFile(fileName, newContent)
        }
    }

    fun createTextFile(rawFileName: String, content: String): WorkspaceDocument {
        val cleanName = ensureExtension(sanitizeFileName(rawFileName), "txt")
        val file = File(workspaceDir, cleanName)
        file.writeText(content)
        onLogEvent(
            AgentCommandType.FILE_WRITE,
            "تم حفظ وتعديل الملف النصي: $cleanName",
            true,
            "File Engine"
        )
        return toWorkspaceDoc(file, DocumentType.TEXT, content)
    }

    /**
     * Generates a real, multi-page standard PDF file using Android's native `PdfDocument` API.
     */
    fun createPdfDocument(
        rawFileName: String,
        title: String,
        bodyText: String
    ): WorkspaceDocument {
        val cleanName = ensureExtension(sanitizeFileName(rawFileName), "pdf")
        val pdfFile = File(workspaceDir, cleanName)
        val sidecar = File(workspaceDir, ".${pdfFile.nameWithoutExtension}.meta.txt")
        sidecar.writeText("$title\n\n$bodyText")

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard points
        val pageHeight = 842

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 25, 47)
            textSize = 20f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 150, 199)
            textSize = 11f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 13f
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 180, 216)
            strokeWidth = 2f
        }

        val lines = wrapTextIntoLines(bodyText, bodyPaint, (pageWidth - 96).toFloat())
        val linesPerPage = 34
        val totalPages = ((lines.size / linesPerPage) + 1).coerceAtLeast(1)

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // White page background
            canvas.drawColor(Color.WHITE)

            var yPos = 56f
            if (pageIndex == 0) {
                canvas.drawText(title, 48f, yPos, titlePaint)
                yPos += 22f
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                canvas.drawText("Created by Z.ai AgentOS | $dateStr", 48f, yPos, subtitlePaint)
                yPos += 14f
                canvas.drawLine(48f, yPos, (pageWidth - 48).toFloat(), yPos, linePaint)
                yPos += 28f
            } else {
                canvas.drawText("$title - الصفحة ${pageIndex + 1}", 48f, yPos, subtitlePaint)
                yPos += 24f
            }

            val startLine = pageIndex * linesPerPage
            val endLine = (startLine + linesPerPage).coerceAtMost(lines.size)
            for (i in startLine until endLine) {
                canvas.drawText(lines[i], 48f, yPos, bodyPaint)
                yPos += 20f
            }

            // Footer
            canvas.drawText(
                "Page ${pageIndex + 1} of $totalPages — Z.ai Autonomous Document Studio",
                48f,
                (pageHeight - 36).toFloat(),
                subtitlePaint
            )

            pdfDocument.finishPage(page)
        }

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        onLogEvent(
            AgentCommandType.CREATE_PDF,
            "تم إنشاء مستند PDF رسمي: $cleanName ($totalPages صفحة)",
            true,
            "Document Studio"
        )
        return toWorkspaceDoc(pdfFile, DocumentType.PDF, "$title\n\n$bodyText")
    }

    /**
     * Generates a genuine OpenXML `.docx` Word document (valid ZIP archive with `[Content_Types].xml`,
     * `_rels/.rels`, and `word/document.xml`) that opens in Microsoft Word, Google Docs, and WPS Office.
     */
    fun createWordDocxDocument(
        rawFileName: String,
        title: String,
        bodyText: String
    ): WorkspaceDocument {
        val cleanName = ensureExtension(sanitizeFileName(rawFileName), "docx")
        val docxFile = File(workspaceDir, cleanName)

        val paragraphs = (listOf(title) + bodyText.split("\n")).filter { it.isNotBlank() }

        val contentTypesXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val relsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()

        val bodyXmlBuilder = StringBuilder()
        paragraphs.forEachIndexed { idx, para ->
            val escaped = escapeXml(para)
            if (idx == 0) {
                bodyXmlBuilder.append(
                    """
                    <w:p>
                        <w:pPr><w:bidi/></w:pPr>
                        <w:r>
                            <w:rPr><w:b/><w:sz w:val="32"/><w:color w:val="0077B6"/></w:rPr>
                            <w:t>$escaped</w:t>
                        </w:r>
                    </w:p>
                    """.trimIndent()
                )
            } else {
                bodyXmlBuilder.append(
                    """
                    <w:p>
                        <w:pPr><w:bidi/></w:pPr>
                        <w:r>
                            <w:rPr><w:sz w:val="24"/></w:rPr>
                            <w:t>$escaped</w:t>
                        </w:r>
                    </w:p>
                    """.trimIndent()
                )
            }
        }

        val wordDocumentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    $bodyXmlBuilder
                    <w:sectPr>
                        <w:pgSz w:w="11906" w:h="16838"/>
                        <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
                    </w:sectPr>
                </w:body>
            </w:document>
        """.trimIndent()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(docxFile))).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)
            writeZipEntry(zos, "_rels/.rels", relsXml)
            writeZipEntry(zos, "word/document.xml", wordDocumentXml)
        }

        onLogEvent(
            AgentCommandType.CREATE_WORD,
            "تم إنشاء مستند Word (.docx) احترافي: $cleanName",
            true,
            "Document Studio"
        )
        return toWorkspaceDoc(docxFile, DocumentType.WORD, "$title\n\n$bodyText")
    }

    /**
     * Generates a genuine OpenXML `.xlsx` Excel spreadsheet (valid ZIP archive with worksheet XML)
     * that opens directly in Microsoft Excel, Google Sheets, and Calc.
     */
    fun createExcelXlsxDocument(
        rawFileName: String,
        sheetTitle: String,
        rows: List<List<String>>
    ): WorkspaceDocument {
        val cleanName = ensureExtension(sanitizeFileName(rawFileName), "xlsx")
        val xlsxFile = File(workspaceDir, cleanName)

        val contentTypesXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
        """.trimIndent()

        val rootRelsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()

        val safeSheetName = escapeXml(sheetTitle.take(28).ifBlank { "Sheet1" })
        val workbookXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets>
                    <sheet name="$safeSheetName" sheetId="1" r:id="rId1"/>
                </sheets>
            </workbook>
        """.trimIndent()

        val workbookRelsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent()

        val sheetDataBuilder = StringBuilder()
        rows.forEachIndexed { rowIdx, rowCells ->
            val rNum = rowIdx + 1
            sheetDataBuilder.append("""<row r="$rNum">""")
            rowCells.forEachIndexed { colIdx, cellValue ->
                val colLetter = ('A' + (colIdx % 26))
                val cellRef = "$colLetter$rNum"
                val trimmed = cellValue.trim()
                val numericVal = trimmed.toDoubleOrNull()
                if (numericVal != null) {
                    sheetDataBuilder.append("""<c r="$cellRef" t="n"><v>$numericVal</v></c>""")
                } else {
                    val escaped = escapeXml(trimmed)
                    sheetDataBuilder.append("""<c r="$cellRef" t="inlineStr"><is><t>$escaped</t></is></c>""")
                }
            }
            sheetDataBuilder.append("""</row>""")
        }

        val sheetXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetViews>
                    <sheetView rightToLeft="1" workbookViewId="0"/>
                </sheetViews>
                <sheetData>
                    $sheetDataBuilder
                </sheetData>
            </worksheet>
        """.trimIndent()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(xlsxFile))).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)
            writeZipEntry(zos, "_rels/.rels", rootRelsXml)
            writeZipEntry(zos, "xl/workbook.xml", workbookXml)
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml)
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml)
        }

        val textRepresentation = rows.joinToString("\n") { it.joinToString(" | ") }
        onLogEvent(
            AgentCommandType.CREATE_EXCEL,
            "تم إنشاء جدول Excel (.xlsx) متكامل: $cleanName (${rows.size} صفوف)",
            true,
            "Document Studio"
        )
        return toWorkspaceDoc(xlsxFile, DocumentType.EXCEL, textRepresentation)
    }

    fun deleteDocument(doc: WorkspaceDocument): Boolean {
        val file = File(doc.filePath)
        val ok = file.delete()
        if (ok) {
            onLogEvent(
                AgentCommandType.FILE_WRITE,
                "تم حذف الملف: ${doc.fileName}",
                true,
                "File Engine"
            )
        }
        return ok
    }

    fun openDocumentExternally(doc: WorkspaceDocument) {
        try {
            val file = File(doc.filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, doc.docType.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "فتح ${doc.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            shareDocument(doc)
        }
    }

    fun shareDocument(doc: WorkspaceDocument) {
        try {
            val file = File(doc.filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = doc.docType.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, doc.fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاركة ${doc.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    fun parseTextToTableRows(rawText: String): List<List<String>> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return listOf(
                listOf("البند", "القيمة", "الحالة"),
                listOf("المهمة الأولى", "100%", "مكتمل"),
                listOf("المهمة الثانية", "85%", "قيد التنفيذ")
            )
        }
        return lines.map { line ->
            when {
                line.contains("|") -> line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                line.contains(",") -> line.split(",").map { it.trim() }
                line.contains("\t") -> line.split("\t").map { it.trim() }
                else -> listOf(line)
            }
        }
    }

    private fun extractTextFromDocxFile(file: File): String {
        return try {
            val sb = StringBuilder()
            ZipInputStream(file.inputStream()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val xml = zis.bufferedReader().readText()
                        val regex = Regex("<w:t[^>]*>(.*?)</w:t>")
                        regex.findAll(xml).forEach { match ->
                            sb.append(unescapeXml(match.groupValues[1])).append("\n")
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
            sb.toString().trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractTextFromXlsxFile(file: File): String {
        return try {
            val rows = mutableListOf<String>()
            ZipInputStream(file.inputStream()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith("xl/worksheets/sheet")) {
                        val xml = zis.bufferedReader().readText()
                        val rowRegex = Regex("<row[^>]*>(.*?)</row>")
                        val cellRegex = Regex("<t[^>]*>(.*?)</t>|<v>(.*?)</v>")
                        rowRegex.findAll(xml).forEach { rowMatch ->
                            val cells = cellRegex.findAll(rowMatch.groupValues[1]).map {
                                val tVal = it.groupValues[1]
                                val vVal = it.groupValues[2]
                                unescapeXml(if (tVal.isNotEmpty()) tVal else vVal)
                            }.toList()
                            if (cells.isNotEmpty()) {
                                rows.add(cells.joinToString(" | "))
                            }
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
            rows.joinToString("\n")
        } catch (_: Exception) {
            ""
        }
    }

    private fun wrapTextIntoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        for (paragraph in text.split("\n")) {
            if (paragraph.isBlank()) {
                result.add("")
                continue
            }
            val words = paragraph.split(" ")
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = testLine
                } else {
                    if (currentLine.isNotEmpty()) result.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) result.add(currentLine)
        }
        return result
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun toWorkspaceDoc(file: File, docType: DocumentType, preview: String): WorkspaceDocument {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return WorkspaceDocument(
            id = file.name,
            fileName = file.name,
            filePath = file.absolutePath,
            docType = docType,
            sizeBytes = file.length(),
            lastModified = fmt.format(Date(file.lastModified())),
            previewContent = preview
        )
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
        return cleaned.ifBlank { "ملف_وكيل_${System.currentTimeMillis()}" }
    }

    private fun ensureExtension(name: String, ext: String): String {
        return if (name.lowercase().endsWith(".$ext")) name else "$name.$ext"
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun unescapeXml(input: String): String {
        return input
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }
}
