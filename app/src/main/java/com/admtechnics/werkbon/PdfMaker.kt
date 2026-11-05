package com.admtechnics.werkbon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfMaker {

    private const val PW = 595
    private const val PH = 842
    private const val MARGIN = 28f

    private val primary = Color.parseColor("#0F172A")
    private val accent = Color.parseColor("#2563EB")
    private val lightBg = Color.parseColor("#F8FAFC")
    private val altBg = Color.parseColor("#F1F5F9")
    private val border = Color.parseColor("#CBD5E1")
    private val textDark = Color.parseColor("#1E293B")
    private val white = Color.WHITE

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")

    data class PdfResult(val file: File, val displayName: String)

    @Volatile
    var admContextHolder: Context? = null

    fun makePdf(
        ctx: Context,
        state: AppState,
        projectLabel: String,
        weekEntries: List<WorkEntry>,
        weekExpenses: List<Expense>,
        year: Int,
        week: Int
    ): PdfResult {
        val projEntries = weekEntries.filter { it.project == projectLabel }
        val projExpenses = weekExpenses.filter { it.project == projectLabel }

        val doc = PdfDocument()
        var pageNum = 1

        var page = doc.startPage(PdfDocument.PageInfo.Builder(PW, PH, pageNum++).create())
        var canvas = page.canvas

        var y = MARGIN
        y = drawHeader(canvas, state, projectLabel, year, week, y)
        y += 14f

        val dayNamesNl = listOf("Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrijdag")
        val dayOrder = mapOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5)

        val sorted = projEntries.sortedWith(
            compareBy({ it.worker }, { dayOrder[it.day] ?: 9 })
        )

        y = drawWorkerTable(canvas, state, sorted, dayNamesNl, year, week, y)
        y += 14f

        val sortedExp = projExpenses.sortedBy { dayOrder[it.day] ?: 9 }
        val expWithCode = sortedExp.mapIndexed { idx, e -> e to "Doc ${idx + 1}" }
        if (expWithCode.isNotEmpty()) {
            y = drawExpenseTable(canvas, expWithCode, year, week, y)
            y += 14f
        }

        y = drawSummary(canvas, state, sorted, expWithCode, y)

        doc.finishPage(page)

        val withImg = expWithCode.filter { it.first.image.isNotEmpty() }
        if (withImg.isNotEmpty()) {
            val chunks = withImg.chunked(4)
            for ((chunkIdx, chunk) in chunks.withIndex()) {
                val p = doc.startPage(PdfDocument.PageInfo.Builder(PW, PH, pageNum++).create())
                val c = p.canvas
                var yy = MARGIN
                yy = drawAttachmentsHeader(c, chunkIdx, yy)
                yy += 10f
                drawImageGrid(c, chunk, yy)
                doc.finishPage(p)
            }
        }

        val outDir = File(ctx.filesDir, "pdfs").apply { mkdirs() }
        val cleanProj = projectLabel.replace(" ", "_").replace("(", "").replace(")", "")
        val fname = "Werkbon_W${week}${year}_${cleanProj}.pdf"
        val out = File(outDir, fname)
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()

        return PdfResult(out, fname)
    }

    private fun drawHeader(canvas: Canvas, state: AppState, projectLabel: String, year: Int, week: Int, top: Float): Float {
        val boxH = 95f // Yüksekliği biraz artırdık
        paint.style = Paint.Style.FILL
        paint.color = lightBg
        canvas.drawRect(MARGIN, top, PW - MARGIN, top + boxH, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = accent
        canvas.drawRect(MARGIN, top, PW - MARGIN, top + boxH, paint)

        var x = MARGIN + 10f
        var yLine = top + 22f

        // Logo
        val logoBmp = loadLogo(state.compLogoPath)
        if (logoBmp != null) {
            val logoSize = 45f
            canvas.drawBitmap(logoBmp, null, RectF(x, top + 20f, x + logoSize, top + 20f + logoSize), null)
            x += logoSize + 12f
        }

        // Firma Bilgileri
        paint.color = primary
        boldPaint.color = primary
        boldPaint.textSize = 11f
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(state.compName, x, yLine, boldPaint)
        
        yLine += 16f // Satır aralığını açtık
        paint.color = Color.parseColor("#475569")
        paint.textSize = 8.5f
        canvas.drawText(state.compAddress, x, yLine, paint)
        
        yLine += 13f
        canvas.drawText("E-mail: ${state.compEmail} | GSM: ${state.compGsm}", x, yLine, paint)
        
        yLine += 13f
        canvas.drawText("BTW: ${state.compBtw}", x, yLine, paint)

        // Sağ Taraf (Hafta ve Proje)
        val rightX = PW - MARGIN - 180f 
        
        // Hafta
        boldPaint.color = primary
        boldPaint.textSize = 9f
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Weeknummer:", rightX, top + 26f, boldPaint)
        paint.color = textDark
        paint.textSize = 9f
        canvas.drawText("$week ($year)", rightX + 85f, top + 26f, paint)

        // Proje
        boldPaint.color = primary
        canvas.drawText("Project:", rightX, top + 52f, boldPaint)
        
        // Uzun proje isimleri taşmasın diye kırpma
        paint.color = textDark
        val projectText = projectLabel
        val maxWidth = PW - MARGIN - (rightX + 85f) - 5f
        val fittedText = fitText(projectText, paint, maxWidth)
        canvas.drawText
