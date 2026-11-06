package com.admtechnics.werkbon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProformaScreen(state: AppState, onChange: (AppState) -> Unit) {
    var selectedProject by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    // Geçici Proforma Kalemleri
    var items by remember { mutableStateOf(listOf<ProformaItem>()) }
    var newDesc by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("1") }
    var newPrice by remember { mutableStateOf("") }

    val projects = state.projects.map { "${it.code} - ${it.name}" }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        
        // YENİ PROFORMA OLUŞTURMA
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Title("Yeni Proforma Oluştur")
                
                Picker("Proje Seçin", if (selectedProject.isEmpty()) "Seçiniz" else selectedProject, listOf("Seçiniz") + projects, { 
                    if (it != "Seçiniz") selectedProject = it else selectedProject = ""
                }, Modifier.fillMaxWidth())
                
                Spacer(Modifier.height(12.dp))
                
                // Kalem Ekleme Alanı
                Text("Kalemler", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.description, fontWeight = FontWeight.Medium)
                            Text("${item.quantity} x €${item.price}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { items = items.toMutableList().also { it.removeAt(index) } }) {
                            Text("❌")
                        }
                    }
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Açıklama") }, modifier = Modifier.weight(2f), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = newQty, onValueChange = { newQty = it }, label = { Text("Adet") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = newPrice, onValueChange = { newPrice = it }, label = { Text("Fiyat (€)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newDesc.isNotBlank() && newPrice.isNotBlank()) {
                            val q = newQty.toDoubleOrNull() ?: 1.0
                            val p = newPrice.replace(',', '.').toDoubleOrNull() ?: 0.0
                            items = items + ProformaItem(newDesc, q, p)
                            newDesc = ""; newQty = "1"; newPrice = ""
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("+ Kalem Ekle") }

                Spacer(Modifier.height(16.dp))
                Field("Notlar (İsteğe Bağlı)", note) { note = it }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (selectedProject.isNotEmpty() && items.isNotEmpty()) {
                            val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                            val newProf = Proforma(
                                id = System.currentTimeMillis().toString(),
                                projectName = selectedProject,
                                date = dateStr,
                                items = items,
                                note = note
                            )
                            onChange(state.copy(proformas = state.proformas + newProf))
                            
                            // Formu sıfırla
                            selectedProject = ""
                            items = emptyList()
                            note = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Proformayı Kaydet") }
            }
        }

        // GEÇMİŞ PROFORMALAR
        Title("Geçmiş Proformalar")
        if (state.proformas.isEmpty()) {
            Text("Henüz proforma oluşturulmadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.proformas.reversed().forEach { prof ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(prof.projectName, fontWeight = FontWeight.Bold)
                            Text("Tarih: ${prof.date} | Kalem: ${prof.items.size}", fontSize = 12.sp)
                            val total = prof.items.sumOf { it.quantity * it.price }
                            Text("Toplam: €%.2f".format(total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            val newList = state.proformas.toMutableList()
                            newList.remove(prof)
                            onChange(state.copy(proformas = newList))
                        }) {
                            Text("🗑️")
                        }
                    }
                }
            }
        }
    }
}
