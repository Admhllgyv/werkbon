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
        canvas.drawText(fittedText, rightX + 85f, top + 52f, paint)

        return top + boxH
    }

    // Yazı sığmazsa sonuna "..." ekler
    private fun fitText(text: String, p: Paint, maxWidth: Float): String {
        if (p.measureText(text) <= maxWidth) return text
        var s = text
        while (s.isNotEmpty() && p.measureText("$s...") > maxWidth) {
            s = s.dropLast(1)
        }
        return "$s..."
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
        val cols = floatArrayOf(95f, 115f, 55f, 55f, 55f, 60f)
        val rowH = 18f // Satır yüksekliğini biraz artırdık (16->18)
        val x0 = MARGIN
        var y = top

        paint.style = Paint.Style.FILL
        paint.color = primary
        canvas.drawRect(x0, y, PW - MARGIN, y + rowH, paint)
        boldPaint.color = white
        boldPaint.textSize = 8.5f
        boldPaint.textAlign = Paint.Align.CENTER
        val heads = listOf("Medewerker", "Dag", "Start", "Pauze", "Einde", "Uren")
        var x = x0
        for (i in heads.indices) {
            val cx = x + cols[i] / 2f
            canvas.drawText(heads[i], cx, y + rowH - 6f, boldPaint)
            x += cols[i]
        }
        y += rowH

        val mon = mondayOf(year, week)
        for ((idx, e) in entries.withIndex()) {
            paint.style = Paint.Style.FILL
            paint.color = if (idx % 2 == 0) white else lightBg
            canvas.drawRect(x0, y, PW - MARGIN, y + rowH, paint)

            paint.style = Paint.Style.STROKE
            paint.color = border
            paint.strokeWidth = 0.5f
            canvas.drawRect(x0, y, PW - MARGIN, y + rowH, paint)

            val d = mon.plusDays(e.day.toLong())
            val dateStr = "${dayNl[e.day]} (${d.format(dateFmt)})"

            paint.color = textDark
            paint.textSize = 8f
            var xx = x0 + 4f

            boldPaint.color = textDark
            boldPaint.textSize = 8f
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(e.worker, xx, y + rowH - 6f, boldPaint)
            xx += cols[0]

            canvas.drawText(dateStr, xx + 4f, y + rowH - 6f, paint); xx += cols[1]
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(hm(e.start), xx + cols[2] / 2f, y + rowH - 6f, paint); xx += cols[2]
            canvas.drawText(hm(e.pause), xx + cols[3] / 2f, y + rowH - 6f, paint); xx += cols[3]
            canvas.drawText(hm(e.end), xx + cols[4] / 2f, y + rowH - 6f, paint); xx += cols[4]
            canvas.drawText(fmt(e.hours()), xx + cols[5] / 2f, y + rowH - 6f, paint)

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
        val cols = floatArrayOf(70f, 105f, 200f, 60f)
        val rowH = 18f // Satır yüksekliği artırıldı
        var y = top

        boldPaint.color = accent
        boldPaint.textSize = 10f
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Materialen en Kosten:", MARGIN, y, boldPaint)
        y += 8f

        paint.style = Paint.Style.FILL
        paint.color = altBg
        canvas.drawRect(MARGIN, y, PW - MARGIN, y + rowH, paint)
        paint.style = Paint.Style.STROKE
        paint.color = border
        paint.strokeWidth = 0.5f
        canvas.drawRect(MARGIN, y, PW - MARGIN, y + rowH, paint)

        boldPaint.color = primary
        boldPaint.textSize = 8.5f
        boldPaint.textAlign = Paint.Align.LEFT
        var x = MARGIN + 4f
        canvas.drawText("Document", x, y + rowH - 6f, boldPaint); x += cols[0]
        canvas.drawText("Dag", x, y + rowH - 6f, boldPaint); x += cols[1]
        canvas.drawText("Omschrijving", x, y + rowH - 6f, boldPaint); x += cols[2]
        canvas.drawText("Bedrag (€)", x, y + rowH - 6f, boldPaint)
        y += rowH

        val dayNl = listOf("Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrijdag")
        val mon = mondayOf(year, week)
        for ((e, code) in items) {
            paint.style = Paint.Style.FILL
            paint.color = white
            canvas.drawRect(MARGIN, y, PW - MARGIN, y + rowH, paint)
            paint.style = Paint.Style.STROKE
            paint.color = border
            paint.strokeWidth = 0.5f
            canvas.drawRect(MARGIN, y, PW - MARGIN, y + rowH, paint)

            paint.color = textDark
            paint.textSize = 8f
            paint.textAlign = Paint.Align.LEFT
            var xx = MARGIN + 4f
            boldPaint.color = textDark
            boldPaint.textSize = 8f
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(code, xx, y + rowH - 6f, boldPaint); xx += cols[0]

            val d = mon.plusDays(e.day.toLong())
            canvas.drawText("${dayNl[e.day]} (${d.format(dateFmt)})", xx, y + rowH - 6f, paint); xx += cols[1]
            canvas.drawText(e.category, xx, y + rowH - 6f, paint); xx += cols[2]
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("€%.2f".format(e.amount), xx + cols[3] - 4f, y + rowH - 6f, paint)

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
        val boxH = 95f // Özet kutusu yüksekliği artırıldı
        paint.style = Paint.Style.FILL
        paint.color = lightBg
        canvas.drawRect(MARGIN, top, PW - MARGIN, top + boxH, paint)
        paint.style = Paint.Style.STROKE
        paint.color = border
        paint.strokeWidth = 1f
        canvas.drawRect(MARGIN, top, PW - MARGIN, top + boxH, paint)

        var y = top + 16f
        boldPaint.color = primary
        boldPaint.textSize = 8.5f
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Uren per medewerker:", MARGIN + 8f, y, boldPaint)
        y += 13f

        paint.color = textDark
        paint.textSize = 8.5f
        val workerHours = mutableMapOf<String, Double>()
        for (e in entries) {
            workerHours[e.worker] = (workerHours[e.worker] ?: 0.0) + e.hours()
        }
        for ((w, h) in workerHours) {
            canvas.drawText("• $w: ${fmt(h)} uur", MARGIN + 12f, y, paint)
            y += 11f
        }
        val totalH = workerHours.values.sum()
        val totalCost = expWithCode.sumOf { it.first.amount }

        val rightColX = MARGIN + 280f
        boldPaint.color = primary
        boldPaint.textSize = 8.5f
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Klantakkoord / Handtekening:", rightColX, top + 16f, boldPaint)

        y = top + boxH - 28f
        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 1f
        canvas.drawLine(rightColX, y, PW - MARGIN - 20f, y, paint)

        paint.color = primary
        boldPaint.color = primary
        boldPaint.textSize = 9f
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Totale uren: ${fmt(totalH)} uur", MARGIN + 8f, top + boxH - 22f, boldPaint)
        canvas.drawText("Totale kosten: €%.2f".format(totalCost), MARGIN + 8f, top + boxH - 10f, boldPaint)

        return top + boxH
    }

    private fun drawAttachmentsHeader(canvas: Canvas, chunkIdx: Int, top: Float): Float {
        val title = if (chunkIdx == 0) "Bijlagen / Bonnetjes:" else "Bijlagen / Bonnetjes (Vervolg):"
        boldPaint.color = accent
        boldPaint.textSize = 11f
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(title, MARGIN, top + 14f, boldPaint)
        return top + 14f
    }

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

            paint.style = Paint.Style.STROKE
            paint.color = border
            paint.strokeWidth = 0.8f
            canvas.drawRect(x, y, x + cellW, y + cellH, paint)

            boldPaint.color = primary
            boldPaint.textSize = 8.5f
            boldPaint.textAlign = Paint.Align.LEFT
            val dayNl = listOf("Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrijdag")
            canvas.drawText("$code - ${e.category} (€%.2f)".format(e.amount), x + 6f, y + 16f, boldPaint)
            paint.color = textDark
            paint.textSize = 8f
            canvas.drawText(dayNl[e.day], x + 6f, y + 28f, paint)

            val file = File(receiptsDir(ctx), e.image)
            if (file.exists()) {
                val bmp = loadScaled(file, cellW.toInt() - 20, cellH.toInt() - 55)
                if (bmp != null) {
                    val imgW = bmp.width.toFloat()
                    val imgH = bmp.height.toFloat()
                    val maxW = cellW - 20f
                    val maxH = cellH - 55f
                    val scale = minOf(maxW / imgW, maxH / imgH, 1f)
                    val drawW = imgW * scale
                    val drawH = imgH * scale
                    val imgX = x + (cellW - drawW) / 2f
                    val imgY = y + 34f + (maxH - drawH) / 2f
                    canvas.drawBitmap(bmp, null, RectF(imgX, imgY, imgX + drawW, imgY + drawH), null)
                }
            }
        }
    }

    private fun loadLogo(path: String): Bitmap? {
        if (path.isBlank()) return null
        val f = File(path)
        if (!f.exists()) return null
        return try { BitmapFactory.decodeFile(f.absolutePath) } catch (e: Exception) { null }
    }

    private fun loadScaled(file: File, maxW: Int, maxH: Int): Bitmap? {
        return try {
            val b = BitmapFactory.Options()
            b.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, b)
            var sample = 1
            while (b.outWidth / sample > maxW || b.outHeight / sample > maxH) sample *= 2
            val o = BitmapFactory.Options()
            o.inSampleSize = sample
            BitmapFactory.decodeFile(file.absolutePath, o)
        } catch (e: Exception) { null }
    }
}
