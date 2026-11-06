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

    private const val PW = 595f
    private const val PH = 842f
    private const val MARGIN = 28f

    private val primary = Color.parseColor("#0F172A")
    private val accent = Color.parseColor("#2563EB")
    private val lightBg = Color.parseColor("#F8FAFC")
    private val altBg = Color.parseColor("#F1F5F9")
    private val border = Color.parseColor("#CBD5E1")
    private val textDark = Color.parseColor("#1E293B")
    private val textMuted = Color.parseColor("#475569")
    private val white = Color.WHITE

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

        var page = doc.startPage(PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), pageNum++).create())
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
                val p = doc.startPage(PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), pageNum++).create())
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

    private fun drawTextNormal(canvas: Canvas, text: String, x: Float, y: Float, size: Float = 8f, color: Int = textDark, align: Paint.Align = Paint.Align.LEFT) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = color
        p.textSize = size
        p.textAlign = align
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(text, x, y, p)
    }

    private fun drawTextBold(canvas: Canvas, text: String, x: Float, y: Float, size: Float = 8f, color: Int = primary, align: Paint.Align = Paint.Align.LEFT) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = color
        p.textSize = size
        p.textAlign = align
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(text, x, y, p)
    }

    private fun drawBackground(canvas: Canvas, rect: RectF, color: Int, isStroke: Boolean = false, strokeWidth: Float = 1f) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = if (isStroke) Paint.Style.STROKE else Paint.Style.FILL
        p.color = color
        p.strokeWidth = strokeWidth
        canvas.drawRect(rect, p)
    }

    private fun drawHeader(canvas: Canvas, state: AppState, projectLabel: String, year: Int, week: Int, top: Float): Float {
        val boxH = 105f
        drawBackground(canvas, RectF(MARGIN, top, PW - MARGIN, top + boxH), lightBg)
        drawBackground(canvas, RectF(MARGIN, top, PW - MARGIN, top + boxH), accent, isStroke = true, strokeWidth = 2f)

        var x = MARGIN + 10f
        var yLine = top + 22f

        val logoBmp = loadLogo(state.compLogoPath)
        if (logoBmp != null) {
            val logoSize = 45f
            canvas.drawBitmap(logoBmp, null, RectF(x, top + 20f, x + logoSize, top + 20f + logoSize), null)
            x += logoSize + 12f
        }

        drawTextBold(canvas, state.compName, x, yLine, 11f)
        
        yLine += 16f
        drawTextNormal(canvas, state.compAddress, x, yLine, 8f, textMuted)
        
        yLine += 13f
        drawTextNormal(canvas, "E-mail: ${state.compEmail} | GSM: ${state.compGsm}", x, yLine, 8f, textMuted)
        
        yLine += 13f
        drawTextNormal(canvas, "BTW: ${state.compBtw}", x, yLine, 8f, textMuted)

        val rightLabelX = PW - MARGIN - 180f
        val rightValueX = PW - MARGIN - 85f
        
        drawTextBold(canvas, "Weeknummer:", rightLabelX, top + 26f, 8.5f)
        drawTextNormal(canvas, "$week ($year)", rightValueX, top + 26f, 8.5f)

        drawTextBold(canvas, "Project:", rightLabelX, top + 52f, 8.5f)
        
        val maxWidth = (PW - MARGIN) - rightValueX - 5f
        val lines = wrapText(projectLabel, maxWidth, 8.5f)
        
        if (lines.isNotEmpty()) {
            drawTextNormal(canvas, lines[0], rightValueX, top + 52f, 8.5f)
        }
        if (lines.size > 1) {
            drawTextNormal(canvas, lines[1], rightValueX, top + 64f, 8.5f)
        }

        return top + boxH
    }

    private fun wrapText(text: String, maxWidth: Float, textSize: Float): List<String> {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = textSize
        p.typeface = Typeface.DEFAULT
        
        if (p.measureText(text) <= maxWidth) return listOf(text)
        
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (p.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    private fun drawWorkerTable(
        canvas: Canvas,
        state: AppState,
        entries: List<WorkEntry>,
        dayNl: List<String>,
        year: Int,
        week: Int,
        top: Float
    ): Float {
        val cols = floatArrayOf(90f, 120f, 55f, 55f, 55f, 165f) 
        val rowH = 18f
        val x0 = MARGIN
        var y = top

        drawBackground(canvas, RectF(x0, y, PW - MARGIN, y + rowH), primary)
        
        val heads = listOf("Medewerker", "Dag", "Start", "Pauze", "Einde", "Uren")
        var x = x0
        for (i in heads.indices) {
            drawTextBold(canvas, heads[i], x + cols[i] / 2f, y + rowH - 6f, 8.5f, white, Paint.Align.CENTER)
            x += cols[i]
        }
        y += rowH

        val mon = mondayOf(year, week)
        for ((idx, e) in entries.withIndex()) {
            val bgColor = if (idx % 2 == 0) white else lightBg
            drawBackground(canvas, RectF(x0, y, PW - MARGIN, y + rowH), bgColor)
            drawBackground(canvas, RectF(x0, y, PW - MARGIN, y + rowH), border, isStroke = true, strokeWidth = 0.5f)

            val d = mon.plusDays(e.day.toLong())
            val dateStr = "${dayNl[e.day]} (${d.format(dateFmt)})"

            var xx = x0

            drawTextBold(canvas, e.worker, xx + 6f, y + rowH - 6f, 8f)
            xx += cols[0]

            drawTextNormal(canvas, dateStr, xx + 6f, y + rowH - 6f, 8f)
            xx += cols[1]

            drawTextNormal(canvas, hm(e.start), xx + cols[2] / 2f, y + rowH - 6f, 8f, textDark, Paint.Align.CENTER); xx += cols[2]
            drawTextNormal(canvas, hm(e.pause), xx + cols[3] / 2f, y + rowH - 6f, 8f, textDark, Paint.Align.CENTER); xx += cols[3]
            drawTextNormal(canvas, hm(e.end), xx + cols[4] / 2f, y + rowH - 6f, 8f, textDark, Paint.Align.CENTER); xx += cols[4]
            
            drawTextNormal(canvas, fmt(e.hours()), xx + cols[5] / 2f, y + rowH - 6f, 8f, textDark, Paint.Align.CENTER)

            y += rowH
        }

        return y
    }

    private fun drawExpenseTable(
        canvas: Canvas,
        items: List<Pair<Expense, String>>,
        year: Int,
        week: Int,
        top: Float
    ): Float {
        val cols = floatArrayOf(70f, 110f, 260f, 100f)
        val rowH = 18f
        var y = top

        drawTextBold(canvas, "Materialen en Kosten:", MARGIN, y, 10f, accent)
        y += 8f

        drawBackground(canvas, RectF(MARGIN, y, PW - MARGIN, y + rowH), altBg)
        drawBackground(canvas, RectF(MARGIN, y, PW - MARGIN, y + rowH), border, isStroke = true, strokeWidth = 0.5f)

        var x = MARGIN + 6f
        drawTextBold(canvas, "Document", x, y + rowH - 6f, 8.5f); x += cols[0]
        drawTextBold(canvas, "Dag", x, y + rowH - 6f, 8.5f); x += cols[1]
        drawTextBold(canvas, "Omschrijving", x, y + rowH - 6f, 8.5f); x += cols[2]
        drawTextBold(canvas, "Bedrag (€)", x, y + rowH - 6f, 8.5f)
        y += rowH

        val dayNl = listOf("Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrijdag")
        val mon = mondayOf(year, week)
        for ((e, code) in items) {
            drawBackground(canvas, RectF(MARGIN, y, PW - MARGIN, y + rowH), white)
            drawBackground(canvas, RectF(MARGIN, y, PW - MARGIN, y + rowH), border, isStroke = true, strokeWidth = 0.5f)

            var xx = MARGIN + 6f
            
            drawTextBold(canvas, code, xx, y + rowH - 6f, 8f); xx += cols[0]

            val d = mon.plusDays(e.day.toLong())
            drawTextNormal(canvas, "${dayNl[e.day]} (${d.format(dateFmt)})", xx, y + rowH - 6f, 8f); xx += cols[1]
            drawTextNormal(canvas, e.category, xx, y + rowH - 6f, 8f); xx += cols[2]
            
            drawTextNormal(canvas, "€%.2f".format(e.amount), xx + cols[3] - 6f, y + rowH - 6f, 8f, textDark, Paint.Align.RIGHT)

            y += rowH
        }
        return y
    }

    private fun drawSummary(
        canvas: Canvas,
        state: AppState,
        entries: List<WorkEntry>,
        expWithCode: List<Pair<Expense, String>>,
        top: Float
    ): Float {
        val boxH = 95f
        drawBackground(canvas, RectF(MARGIN, top, PW - MARGIN, top + boxH), lightBg)
        drawBackground(canvas, RectF(MARGIN, top, PW - MARGIN, top + boxH), border, isStroke = true, strokeWidth = 1f)

        var y = top + 18f
        drawTextBold(canvas, "Uren per medewerker:", MARGIN + 10f, y, 8.5f)
        y += 14f

        val workerHours = mutableMapOf<String, Double>()
        for (e in entries) {
            workerHours[e.worker] = (workerHours[e.worker] ?: 0.0) + e.hours()
        }
        for ((w, h) in workerHours) {
            drawTextNormal(canvas, "• $w: ${fmt(h)} uur", MARGIN + 14f, y, 8.5f)
            y += 12f
        }
        val totalH = workerHours.values.sum()
        val totalCost = expWithCode.sumOf { it.first.amount }

        val rightColX = MARGIN + 280f
        drawTextBold(canvas, "Klantakkoord / Handtekening:", rightColX, top + 18f, 8.5f)

        y = top + boxH - 30f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint.color = Color.parseColor("#94A3B8")
        linePaint.strokeWidth = 1f
        canvas.drawLine(rightColX, y, PW - MARGIN - 20f, y, linePaint)

        drawTextBold(canvas, "Totale uren: ${fmt(totalH)} uur", MARGIN + 10f, top + boxH - 22f, 9f)
        drawTextBold(canvas, "Totale kosten: €%.2f".format(totalCost), MARGIN + 10f, top + boxH - 10f, 9f)

        return top + boxH
    }

    private fun drawAttachmentsHeader(canvas: Canvas, chunkIdx: Int, top: Float): Float {
        val title = if (chunkIdx == 0) "Bijlagen / Bonnetjes:" else "Bijlagen / Bonnetjes (Vervolg):"
        drawTextBold(canvas, title, MARGIN, top + 14f, 11f, accent)
        return top + 14f
    }

    // YENİ: Resim çözünürlüğü optimize edildi (Hız için)
    private fun drawImageGrid(canvas: Canvas, items: List<Pair<Expense, String>>, top: Float) {
        val ctx = admContextHolder ?: return
        val cellW = (PW - 2 * MARGIN) / 2f
        val cellH = 360f
        
        for ((i, pair) in items.withIndex()) {
            val (e, code) = pair
            val col = i % 2
            val row = i / 2
            val x = MARGIN + col * cellW
            val y = top + row * cellH

            drawBackground(canvas, RectF(x, y, x + cellW, y + cellH), white)
            drawBackground(canvas, RectF(x, y, x + cellW, y + cellH), border, isStroke = true, strokeWidth = 0.8f)

            val dayNl = listOf("Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrijdag")
            drawTextBold(canvas, "$code - ${e.category} (€%.2f)".format(e.amount), x + 6f, y + 16f, 8.5f)
            drawTextNormal(canvas, dayNl[e.day], x + 6f, y + 28f, 8f, textMuted)

            val imgArea = RectF(x + 5f, y + 34f, x + cellW - 5f, y + cellH - 5f)

            val file = File(receiptsDir(ctx), e.image)
            if (file.exists()) {
                // Bellek dostu ve hızlı yükleme için "inSampleSize" kullanıyoruz
                val bmp = decodeSampledBitmapFromFile(file, imgArea.width().toInt(), imgArea.height().toInt())
                
                if (bmp != null) {
                    val scale = minOf(imgArea.width() / bmp.width, imgArea.height() / bmp.height)
                    val drawW = bmp.width * scale
                    val drawH = bmp.height * scale
                    
                    val imgX = imgArea.left + (imgArea.width() - drawW) / 2f
                    val imgY = imgArea.top + (imgArea.height() - drawH) / 2f
                    
                    val destRect = RectF(imgX, imgY, imgX + drawW, imgY + drawH)
                    
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                    canvas.drawBitmap(bmp, null, destRect, paint)
                    bmp.recycle()
                }
            }
        }
    }

    private fun decodeSampledBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, options)

        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun loadLogo(path: String): Bitmap? {
        if (path.isBlank()) return null
        val f = File(path)
        if (!f.exists()) return null
        return try { BitmapFactory.decodeFile(f.absolutePath) } catch (e: Exception) { null }
    }
}
