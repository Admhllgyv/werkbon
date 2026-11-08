package com.admtechnics.werkbon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        PremiumCard {
            Title("Yeni Proje Ekle")
            Field("Proje / Müşteri adı", newName) { newName = it }
            Field("Proje kodu (örn. E260320)", newCode) { newCode = it }
            
            if (customerNames.isNotEmpty()) {
                Picker("Firma / Müşteri Seçin", if (selectedCustomer.isEmpty()) "Seçiniz" else selectedCustomer, listOf("Seçiniz") + customerNames, { 
                    if (it != "Seçiniz") selectedCustomer = it else selectedCustomer = ""
                }, Modifier.fillMaxWidth())
            }
            
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                if (newName.isNotBlank()) {
                    val code = newCode.trim().ifEmpty { "E260000" }
                    onChange(state.copy(projects = state.projects + Project(code, newName.trim(), selectedCustomer)))
                    newName = ""; newCode = ""; selectedCustomer = ""
                }
            }, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(8.dp)) { Text("Projeyi Kaydet", fontSize = 13.sp) }
        }

        Title("Mevcut Projeler ve Geçmiş")
        
        state.projects.forEachIndexed { i, p ->
            var isExpanded by remember { mutableStateOf(false) }
            val rotationState by animateFloatAsState(if (isExpanded) 180f else 0f)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            Text("${p.code} - ${p.customerName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, "Detaylar", modifier = Modifier.rotate(rotationState))
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                            Divider(color = Color(0xFFE2E8F0))
                            Spacer(Modifier.height(8.dp))
                            
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onChange(state.copy(projects = state.projects.toMutableList().also { l -> l.removeAt(i) })) }, modifier = Modifier.height(30.dp)) { Text("🗑️ Projeyi Sil", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                            }

                            val projectFullName = "${p.code} - ${p.name}"
                            val projEntriesWithWeek = mutableListOf<Pair<String, WorkEntry>>()
                            
                            // Hafta verisini bulma
                            weeksData.forEach { (weekKey, entries) ->
                                entries.filter { it.project == projectFullName }.forEach { entry ->
                                    projEntriesWithWeek.add(weekKey to entry)
                                }
                            }
                            
                            Text("📋 Projeye Ait Geçmiş Çalışma Kayıtları", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                            
                            if (projEntriesWithWeek.isEmpty()) {
                                Text("Kayıt yok.", fontSize = 12.sp)
                            } else {
                                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))) {
                                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(vertical = 6.dp)) {
                                        TableHeader("Hafta", 1f); TableHeader("Gün", 1f); TableHeader("Çalışan", 1f); TableHeader("Başlangıç", 1f); TableHeader("Bitiş", 1f)
                                    }
                                    projEntriesWithWeek.sortedBy { it.first }.forEachIndexed { index, (weekKey, e) ->
                                        val bgColor = if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        Row(Modifier.fillMaxWidth().background(bgColor).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            TableCell(weekKey.replace("2026-", ""), 1f) // 2026-W40
                                            TableCell(dayTr[e.day], 1f)
                                            TableCell(e.worker, 1f)
                                            TableCell(hm(e.start), 1f)
                                            TableCell(hm(e.end), 1f)
                                        }
                                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            val projExpenses = expensesData.values.flatten().filter { it.project == projectFullName }
                            Text("💶 Projeye Ait Fiş ve Gider Detayları", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                            
                            if (projExpenses.isEmpty()) {
                                Text("Kayıt yok.", fontSize = 12.sp)
                            } else {
                                projExpenses.forEachIndexed { idx, ex ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (ex.image.isNotEmpty()) {
                                                val file = File(receiptsDir(ctx), ex.image)
                                                if (file.exists()) {
                                                    val bmp = decodeSampled(file, 8)
                                                    if (bmp != null) { Image(bitmap = bmp, contentDescription = "Fiş", modifier = Modifier.size(40.dp).padding(end = 8.dp), contentScale = ContentScale.Crop) }
                                                }
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text("Doc ${idx + 1} - ${ex.category}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                Text(dayTr[ex.day], fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("€${ex.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                        }
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
