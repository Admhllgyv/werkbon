package com.admtechnics.werkbon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admtechnics.werkbon.ui.theme.TableHeaderColor
import java.io.File
import java.time.format.DateTimeFormatter

@Composable
fun ProjectsScreen(state: AppState, onChange: (AppState) -> Unit) {
    val ctx = LocalContext.current
    var newName by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf("") }
    
    val customerNames = state.customers.map { it.name }
    val weeksData = WeekStore.load(ctx)
    val expensesData = ExpenseStore.load(ctx)
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        
        // YENİ PROJE EKLEME KARTI
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Title("Yeni Proje Ekle")
                Field("Proje / Müşteri adı", newName) { newName = it }
                Field("Proje kodu (örn. E260320)", newCode) { newCode = it }
                
                if (customerNames.isNotEmpty()) {
                    Picker("Firma / Müşteri Seçin", if (selectedCustomer.isEmpty()) "Seçiniz" else selectedCustomer, listOf("Seçiniz") + customerNames, { 
                        if (it != "Seçiniz") selectedCustomer = it else selectedCustomer = ""
                    }, Modifier.fillMaxWidth())
                }
                
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newName.isNotBlank() && selectedCustomer.isNotEmpty()) {
                            val code = newCode.trim().ifEmpty { "E260000" }
                            onChange(state.copy(projects = state.projects + Project(code, newName.trim(), selectedCustomer)))
                            newName = ""; newCode = ""; selectedCustomer = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Projeyi Kaydet") }
            }
        }

        Title("Mevcut Projeler ve Geçmiş")
        
        state.projects.forEachIndexed { i, p ->
            var isExpanded by remember { mutableStateOf(false) }
            val rotationState by animateFloatAsState(if (isExpanded) 180f else 0f)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { isExpanded = !isExpanded }) {
                Column {
                    // Başlık
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Text("${p.code} - ${p.customerName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, "Detaylar", modifier = Modifier.rotate(rotationState))
                    }

                    // Açılır İçerik
                    AnimatedVisibility(visible = isExpanded) {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(Modifier.height(12.dp))
                            
                            // Proje Bilgileri (Silme vs)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { onChange(state.copy(projects = state.projects.toMutableList().also { l -> l.removeAt(i) })) }) { Text("🗑️ Projeyi Sil", color = MaterialTheme.colorScheme.error) }
                            }

                            val projectFullName = "${p.code} - ${p.name}"
                            
                            // ÇALIŞMA GEÇMİŞİ TABLOSU
                            val projEntries = weeksData.values.flatten().filter { it.project == projectFullName }
                            
                            Text("📋 Projeye Ait Geçmiş Çalışma Kayıtları", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                            
                            if (projEntries.isEmpty()) {
                                Text("Kayıt yok.", fontSize = 12.sp)
                            } else {
                                Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                    Column {
                                        Row(Modifier.background(TableHeaderColor)) {
                                            TableHeader("Hafta", 80.dp)
                                            TableHeader("Gün", 80.dp)
                                            TableHeader("Çalışan", 80.dp)
                                            TableHeader("Başlangıç", 70.dp)
                                            TableHeader("Mola", 60.dp)
                                            TableHeader("Bitiş", 60.dp)
                                            TableHeader("Saat", 60.dp)
                                        }
                                        projEntries.sortedBy { it.day }.forEach { e ->
                                            Row(Modifier.background(MaterialTheme.colorScheme.surface)) {
                                                TableCell("Hafta", 80.dp) // Hafta bilgisi WorkEntry içinde yok, şimdilik placeholder
                                                TableCell(dayTr[e.day], 80.dp)
                                                TableCell(e.worker, 80.dp)
                                                TableCell(hm(e.start), 70.dp)
                                                TableCell(hm(e.pause), 60.dp)
                                                TableCell(hm(e.end), 60.dp)
                                                TableCell(fmt(e.hours()), 60.dp)
                                            }
                                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // FİŞ GEÇMİŞİ (FOTOĞRAFLI)
                            val projExpenses = expensesData.values.flatten().filter { it.project == projectFullName }
                            
                            Text("💶 Projeye Ait Fiş ve Gider Detayları", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                            
                            if (projExpenses.isEmpty()) {
                                Text("Kayıt yok.", fontSize = 12.sp)
                            } else {
                                projExpenses.forEachIndexed { idx, ex ->
                                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("📌 Doc ${idx + 1} | ${ex.category}", fontWeight = FontWeight.Medium)
                                            Spacer(Modifier.weight(1f))
                                            Text("€${ex.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(dayTr[ex.day], fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        
                                        // Fiş Fotoğrafı
                                        if (ex.image.isNotEmpty()) {
                                            val file = File(receiptsDir(ctx), ex.image)
                                            if (file.exists()) {
                                                // Thumb bileşenini çağırıyoruz (Expenses.kt'den)
                                                Thumb(file)
                                            }
                                        }
                                        Divider(Modifier.padding(top = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
