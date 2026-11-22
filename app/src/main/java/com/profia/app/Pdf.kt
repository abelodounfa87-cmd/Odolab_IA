package com.profia.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint

data class MdLine(val text: String, val level: Int)

private fun clean(s: String) = s.replace("**", "").replace("__", "").replace("`", "")

fun parseMd(src: String): List<MdLine> = src.lines().map { raw ->
    val t = raw.trimEnd()
    when {
        t.startsWith("### ") -> MdLine(clean(t.drop(4)), 3)
        t.startsWith("## ") -> MdLine(clean(t.drop(3)), 2)
        t.startsWith("# ") -> MdLine(clean(t.drop(2)), 1)
        t.startsWith("- ") || t.startsWith("* ") -> MdLine("• " + clean(t.drop(2)), 0)
        else -> MdLine(clean(t), 0)
    }
}

/** Écrit le résumé en PDF A4 dans Téléchargements. Retourne le nom du fichier. */
fun exportPdf(context: Context, text: String): String {
    val pageW = 595
    val pageH = 842
    val margin = 48
    val width = pageW - 2 * margin

    val doc = PdfDocument()
    var pageNum = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
    var y = margin.toFloat()

    for (line in parseMd(text)) {
        if (line.text.isBlank()) { y += 6f; continue }
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = when (line.level) { 1 -> 20f; 2 -> 16f; 3 -> 13f; else -> 11f }
            isFakeBoldText = line.level > 0
        }
        val before = if (line.level > 0) 8f else 0f
        val layout = StaticLayout.Builder
            .obtain(line.text, 0, line.text.length, paint, width)
            .setLineSpacing(0f, 1.15f)
            .build()
        if (y + before + layout.height > pageH - margin) {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
            y = margin.toFloat()
        }
        y += before
        val canvas = page.canvas
        canvas.save()
        canvas.translate(margin.toFloat(), y)
        layout.draw(canvas)
        canvas.restore()
        y += layout.height + 4f
    }
    doc.finishPage(page)

    val name = "resume_${System.currentTimeMillis()}.pdf"
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, name)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: throw RuntimeException("Impossible de créer le fichier")
    context.contentResolver.openOutputStream(uri)!!.use { doc.writeTo(it) }
    doc.close()
    return name
}
