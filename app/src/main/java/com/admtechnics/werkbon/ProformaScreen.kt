package com.admtechnics.werkbon

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.temporal.IsoFields
import java.time.LocalDate

@Composable
fun ProformaScreen(state: AppState, onChange: (AppState) -> Unit, title: String) {
    val ctx = LocalContext.current
    
    // Seçim State'leri
    var selectedProject by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf(WeekNav.year) }
    var selectedWeek by remember { mutableStateOf(WeekNav.week) }
    var hourlyRate by remember { mutableStateOf("46,61") } // Varsayılan
    var btwRate by remember { mutableStateOf(0.0) } // Varsayılan %0
    var btwMenuExpanded by remember { mutableStateOf(false) }

    val projects = state.projects.map { "${it.code} - ${it.name}" }
    val projectOptions = listOf("Seçiniz") + projects
    val years = (2024..2030).toList()
    val weeks = (1..53).toList()

    // Seçilen haftanın verilerini çek
    val weekKey = "$selectedYear-W$selectedWeek"
    val weekEntries = WeekStore.load(ctx)[weekKey]?.filter { it.project == selectedProject } ?: emptyList()
    val weekExpenses = ExpenseStore.load(ctx)[weekKey]?.filter { it.project == selectedProject } ?: emptyList()

    val totalHours = r2(weekEntries.sumOf { it.hours() })
    val totalExpenses = r2(weekExpenses.sumOf { it.amount })

    val rateDouble = hourlyRate.replace(',', '.').toDoubleOrNull() ?: 0.0
    val totalLabor = r2(totalHours * rateDouble)
    val subtotal = r2(totalLabor + totalExpenses)
    val btwAmount = r2(subtotal * (btwRate / 100.0))
    val grandTotal = r2(subtotal + btwAmount)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        TopHeaderCard(title)

        PremiumCard {
            Title("Proforma Oluştur")
            
            Picker("Proje Seçin", if (selectedProject.isEmpty()) "Seçiniz" else selectedProject, projectOptions, { 
                if (it != "Seçiniz") selectedProject = it else selectedProject = ""
            }, Modifier.fillMaxWidth())
            
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    Picker("Yıl", selectedYear.toString(), years.map { it.toString() }, { selectedYear = it.toInt() }, Modifier.fillMaxWidth())
                }
                Box(Modifier.weight(1f)) {
                    Picker("Hafta", selectedWeek.toString(), weeks.map { it.toString() }, { selectedWeek = it.toInt() }, Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(8.dp))

            // Otomatik Özet Bilgisi
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(8.dp)) {
                    Text("Seçilen Hafta: $totalHours Saat, $totalExpenses € Gider", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(12.dp))

            Field("Saatlik Ücret (€)", hourlyRate) { hourlyRate = it }

            // BTW (KDV) Seçici
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("BTW (KDV): ", fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Box {
                    OutlinedButton(onClick = { btwMenuExpanded = true }, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.LightGray)) {
                        Text("%${btwRate.toInt()}")
                    }
                    DropdownMenu(expanded = btwMenuExpanded, onDismissRequest = { btwMenuExpanded = false }) {
                        listOf(0.0, 9.0, 21.0).forEach { rate ->
                            DropdownMenuItem(text = { Text("%${rate.toInt()}") }, onClick = { btwRate = rate; btwMenuExpanded = false })
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Hesaplama Özeti
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Uren ($totalHours x €$rateDouble)"); Text("€$totalLabor") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Kosten (Fişler)"); Text("€$totalExpenses") }
                    Divider(Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Subtotaal"); Text("€$subtotal") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("BTW (%${btwRate.toInt()})"); Text("€$btwAmount") }
                    Divider(Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Totaal", fontWeight = FontWeight.Bold); Text("€$grandTotal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedProject.isNotEmpty()) {
                        val pdfFile = createProformaPdfFile(ctx, state, selectedProject, selectedYear, selectedWeek, totalHours, rateDouble, totalLabor, totalExpenses, subtotal, btwRate, btwAmount, grandTotal)
                        showProformaPdfOptions(ctx, pdfFile)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Proforma PDF Oluştur", fontSize = 15.sp) }
        }
    }
}

// PDF OLUŞTURMA VE ÖNİZLEME FONKSİYONLARI
fun createProformaPdfFile(
    ctx: Context, state: AppState, projectFullName: String, year: Int, week: Int,
    totalHours: Double, rate: Double, totalLabor: Double, totalExpenses: Double,
    subtotal: Double, btwRate: Double, btwAmount: Double, grandTotal: Double
): File {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 10f }
    val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f }
    
    var y = 50f
    val leftMargin = 50f
    val rightMargin = 545f

    val proj = state.projects.find { "${it.code} - ${it.name}" == projectFullName }
    val customer = state.customers.find { it.name == proj?.customerName }

    // 1. Üst Kısım (Müşteri ve Firma)
    if (customer != null) {
        canvas.drawText(customer.name, leftMargin, y, boldPaint); y += 15f
        canvas.drawText(customer.address, leftMargin, y, textPaint); y += 15f
        canvas.drawText("BTW: ${customer.btw}", leftMargin, y, textPaint);
    } else {
        canvas.drawText("Müşteri Bilgisi Yok", leftMargin, y, boldPaint)
    }

    y = 50f
    val rightX = 350f
    canvas.drawText(state.compName, rightX, y, boldPaint); y += 15f
    canvas.drawText(state.compAddress, rightX, y, textPaint); y += 15f
    canvas.drawText("BTW: ${state.compBtw}", rightX, y, textPaint); y += 15f
    canvas.drawText("GSM: ${state.compGsm}", rightX, y, textPaint);

    // 2. Orta Kısım (Tarih, Proje, Hafta)
    y = 150f
    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    canvas.drawText(dateStr, leftMargin, y, textPaint)
    
    y += 25f
    canvas.drawText("$projectFullName", leftMargin, y, boldPaint)
    y += 15f
    canvas.drawText("Week $week", leftMargin, y, textPaint)

    // 3. Hesaplama Tablosu
    y += 40f
    canvas.drawText("Gepresteerde uren:", leftMargin, y, boldPaint)
    canvas.drawText("$totalHours uur à €${"%.2f".format(rate)}", 250f, y, textPaint)
    canvas.drawText("€ ${"%.2f".format(totalLabor)}", rightMargin - 50f, y, textPaint)
    
    y += 20f
    canvas.drawText("Kosten:", leftMargin, y, boldPaint)
    canvas.drawText("€ ${"%.2f".format(totalExpenses)}", rightMargin - 50f, y, textPaint)

    // 4. Toplam ve BTW
    y += 60f
    canvas.drawLine(300f, y, rightMargin, y, linePaint)
    y += 20f
    canvas.drawText("Subtotaal", leftMargin, y, textPaint)
    canvas.drawText("€ ${"%.2f".format(subtotal)}", rightMargin - 50f, y, textPaint)

    y += 20f
    if (btwRate == 0.0) {
        canvas.drawText("BTW verlegd 0%", leftMargin, y, textPaint)
        canvas.drawText("€ -", rightMargin - 50f, y, textPaint)
    } else {
        canvas.drawText("BTW ${btwRate.toInt()}%", leftMargin, y, textPaint)
        canvas.drawText("€ ${"%.2f".format(btwAmount)}", rightMargin - 50f, y, textPaint)
    }

    y += 20f
    canvas.drawLine(300f, y, rightMargin, y, linePaint)
    y += 20f
    canvas.drawText("Totaal", leftMargin, y, boldPaint)
    canvas.drawText("€ ${"%.2f".format(grandTotal)}", rightMargin - 50f, y, boldPaint)

    // 5. Alt Notlar
    y += 80f
    val notes = listOf(
        "Manurenlijst toevoegen als PDF toevoegen in mail",
        "Eventuele bonnen van parkeerkosten toevoegen in mail",
        "Factuur Hotelovernachtingen toevoegen in mail",
        "Eventuele werkbon indien deze is verstrekt door FVDL (betreft geen urenstaat of weekstaat)",
        "Materialen middels een apart PDF af leveren."
    )
    notes.forEach { note ->
        canvas.drawText(note, leftMargin, y, textPaint)
        y += 15f
    }

    doc.finishPage(page)
    
    val file = File(ctx.filesDir, "Proforma_${proj?.name?.replace(" ", "_")}_W$week.pdf")
    try {
        val out = FileOutputStream(file); doc.writeTo(out); out.close(); doc.close()
    } catch (e: Exception) { android.widget.Toast.makeText(ctx, "PDF hatası: ${e.message}", android.widget.Toast.LENGTH_LONG).show() }
    
    return file
}

fun showProformaPdfOptions(ctx: Context, file: File) {
    val options = arrayOf("👁️ Görüntüle", "📥 İndir (Kaydet)", "📤 Paylaş")
    android.app.AlertDialog.Builder(ctx).setTitle("Proforma PDF İşlemleri").setItems(options) { _, which ->
        when (which) {
            0 -> {
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY) }
                ctx.startActivity(intent)
            }
            1 -> {
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                ctx.startActivity(Intent.createChooser(intent, "Proforma PDF Kaydet/Gönder"))
            }
            2 -> {
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                ctx.startActivity(Intent.createChooser(intent, "Proforma PDF Paylaş"))
            }
        }
    }.show()
}
