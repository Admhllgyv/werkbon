package com.admtechnics.werkbon

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.BorderStroke
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
fun ProformaScreen(state: AppState, onChange: (AppState) -> Unit, title: String) {
    val ctx = LocalContext.current
    var selectedProject by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<ProformaItem>()) }
    
    var newDesc by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("1") }
    var newPrice by remember { mutableStateOf("") }

    val projects = state.projects.map { "${it.code} - ${it.name}" }
    val projectOptions = listOf("Seçiniz") + projects

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        TopHeaderCard(title)
        
        PremiumCard {
            Title("Yeni Proforma Oluştur")
            
            Picker("Proje Seçin", if (selectedProject.isEmpty()) "Seçiniz" else selectedProject, projectOptions, { 
                if (it != "Seçiniz") selectedProject = it else selectedProject = ""
            }, Modifier.fillMaxWidth())
            
            Spacer(Modifier.height(12.dp))
            
            Text("Kalemler", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            items.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.description, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text("${item.quantity} x €${item.price}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { items = items.toMutableList().also { it.removeAt(index) } }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) { Text("Sil", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                }
            }
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Açıklama", fontSize=11.sp) }, modifier = Modifier.weight(2f).height(56.dp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = newQty, onValueChange = { newQty = it }, label = { Text("Adet", fontSize=11.sp) }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = newPrice, onValueChange = { newPrice = it }, label = { Text("Fiyat (€)", fontSize=11.sp) }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                if (newDesc.isNotBlank() && newPrice.isNotBlank()) {
                    val q = newQty.replace(',', '.').toDoubleOrNull() ?: 1.0
                    val p = newPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
                    items = items + ProformaItem(newDesc, q, p)
                    newDesc = ""; newQty = "1"; newPrice = ""
                }
            }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(36.dp), border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color.LightGray)) { Text("+ Kalem Ekle", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Black) }

            Spacer(Modifier.height(12.dp))
            Field("Notlar (alt kısımda çıkar)", note) { note = it }

            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                if (selectedProject.isNotEmpty() && items.isNotEmpty()) {
                    val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                    val newProf = Proforma(System.currentTimeMillis().toString(), selectedProject, dateStr, items, note)
                    onChange(state.copy(proformas = state.proformas + newProf))
                    selectedProject = ""; items = emptyList(); note = ""
                }
            }, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(8.dp)) { Text("Proformayı Kaydet", fontSize = 13.sp) }
        }

        Title("Geçmiş Proformalar")
        state.proformas.reversed().forEach { prof ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(prof.projectName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Tarih: ${prof.date}", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Gray)
                        val total = prof.items.sumOf { it.quantity * it.price }
                        Text("Toplam: €%.2f".format(total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                    Button(onClick = { createProformaPdf(ctx, state, prof) }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), modifier = Modifier.height(36.dp)) { Text("📄 PDF", fontSize = 12.sp) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { onChange(state.copy(proformas = state.proformas.toMutableList().also { it.remove(prof) })) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(36.dp)) { Text("Sil", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                }
            }
        }
    }
}
