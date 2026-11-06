package com.admtechnics.werkbon

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProformaScreen(state: AppState, onChange: (AppState) -> Unit) {
    val ctx = LocalContext.current
    var selectedProject by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<ProformaItem>()) }
    
    var newDesc by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("1") }
    var newPrice by remember { mutableStateOf("") }

    val projects = state.projects.map { "${it.code} - ${it.name}" }
    val projectOptions = listOf("Seçiniz") + projects

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Title("Yeni Proforma Oluştur")
                
                Picker("Proje Seçin", if (selectedProject.isEmpty()) "Seçiniz" else selectedProject, projectOptions, { 
                    if (it != "Seçiniz") selectedProject = it else selectedProject = ""
                }, Modifier.fillMaxWidth())
                
                Spacer(Modifier.height(12.dp))
                
                Text("Kalemler", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.description, fontWeight = FontWeight.Medium)
                            Text("${item.quantity} x €${item.price}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { items = items.toMutableList().also { it.removeAt(index) } }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
                    }
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Açıklama") }, modifier = Modifier.weight(2f), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = newQty, onValueChange = { newQty = it }, label = { Text("Adet") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = newPrice, onValueChange = { newPrice = it }, label = { Text("Fiyat (€)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    if (newDesc.isNotBlank() && newPrice.isNotBlank()) {
                        val q = newQty.replace(',', '.').toDoubleOrNull() ?: 1.0
                        val p = newPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
                        items = items + ProformaItem(newDesc, q, p)
                        newDesc = ""; newQty = "1"; newPrice = ""
                    }
                }, shape = RoundedCornerShape(8.dp)) { Text("+ Kalem Ekle") }

                Spacer(Modifier.height(16.dp))
                Field("Notlar (alt kısımda çıkar)", note) { note = it }

                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (selectedProject.isNotEmpty() && items.isNotEmpty()) {
                        val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                        val newProf = Proforma(System.currentTimeMillis().toString(), selectedProject, dateStr, items, note)
                        onChange(state.copy(proformas = state.proformas + newProf))
                        selectedProject = ""; items = emptyList(); note = ""
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text("Proformayı Kaydet") }
            }
        }

        Title("Geçmiş Proformalar")
        state.proformas.reversed().forEach { prof ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(prof.projectName, fontWeight = FontWeight.Bold)
                        Text("Tarih: ${prof.date}", fontSize = 12.sp)
                        val total = prof.items.sumOf { it.quantity * it.price }
                        Text("Toplam: €%.2f".format(total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Button(
                        onClick = { createProformaPdf(ctx, state, prof) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("📄 PDF", fontSize = 12.sp) }
                    
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = {
                        val newList = state.proformas.toMutableList()
                        newList.remove(prof)
                        onChange(state.copy(proformas = newList))
                    }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

// PROFORMA PDF OLUŞTURUCU (Görsele Uygun)
fun createProformaPdf(ctx: Context, state: AppState, prof: Proforma) {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 10f }
    val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 1f }
    
    var y = 50f
    val leftMargin = 50f
    val rightMargin = 545f

    val proj = state.projects.find { "${it.code} - ${it.name}" == prof.projectName }
    val customer = state.customers.find { it.name == proj?.customerName }

    // SOL: Müşteri Bilgileri
    if (customer != null) {
        canvas.drawText(customer.name, leftMargin, y, boldPaint); y += 15f
        canvas.drawText(customer.address, leftMargin, y, textPaint); y += 15f
        canvas.drawText("BTW: ${customer.btw}", leftMargin, y, textPaint);
    } else {
        canvas.drawText("Müşteri Bilgisi Yok", leftMargin, y, boldPaint)
    }

    // SAĞ: Firma Bilgileri
    y = 50f
    val rightX = 350f
    canvas.drawText(state.compName, rightX, y, boldPaint); y += 15f
    canvas.drawText(state.compAddress, rightX, y, textPaint); y += 15f
    canvas.drawText("BTW: ${state.compBtw}", rightX, y, textPaint);

    // TARİH
    y = 150f
    canvas.drawText(prof.date, leftMargin, y, textPaint)
    
    // PROJE ADI VE KODU
    y += 40f
    val projName = proj?.name ?: prof.projectName
    val projCode = proj?.code ?: ""
    canvas.drawText("$projName - $projCode", leftMargin, y, boldPaint)
    
    // KALEMLER TABLOSU
    y += 30f
    for (item in prof.items) {
        canvas.drawText(item.description, leftMargin, y, textPaint)
        canvas.drawText("${item.quantity} x €${"%.2f".format(item.price)}", 300f, y, textPaint)
        canvas.drawText("€ ${"%.2f".format(item.quantity * item.price)}", rightMargin - 50f, y, textPaint)
        y += 15f
    }

    // TOPLAM
    y += 20f
    val total = prof.items.sumOf { it.quantity * it.price }
    canvas.drawLine(300f, y, rightMargin, y, linePaint)
    y += 20f
    canvas.drawText("Totaal", leftMargin, y, boldPaint)
    canvas.drawText("€ ${"%.2f".format(total)}", rightMargin - 50f, y, boldPaint)

    // NOTLAR
    if (prof.note.isNotBlank()) {
        y += 60f
        prof.note.lines().forEach { line ->
            canvas.drawText(line, leftMargin, y, textPaint)
            y += 15f
        }
    }

    // ALT BİLGİ
    y = 750f
    canvas.drawText("Bedankt voor uw vertrouwen!", leftMargin, y, boldPaint)

    doc.finishPage(page)
    
    // Dosyayı oluştur ve paylaş
    val file = File(ctx.filesDir, "Proforma_${prof.projectName.replace(" ", "_")}.pdf")
    try {
        val out = FileOutputStream(file)
        doc.writeTo(out)
        out.close()
        doc.close()
        
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Proforma PDF Paylaş"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(ctx, "PDF hatası: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
