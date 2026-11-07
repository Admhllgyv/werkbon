package com.admtechnics.werkbon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter

@Composable
fun OzetScreen(state: AppState) {
    val ctx = LocalContext.current
    var year by remember { mutableStateOf(WeekNav.year) }
    var week by remember { mutableStateOf(WeekNav.week) }

    val key = "$year-W$week"
    val weeksData = WeekStore.load(ctx)
    val expensesData = ExpenseStore.load(ctx)

    val entries = weeksData[key] ?: emptyList()
    val expenses = expensesData[key] ?: emptyList()

    val mon = mondayOf(year, week)
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun shift(delta: Long) {
        val m = mondayOf(year, week).plusWeeks(delta)
        year = m.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
        week = m.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        
        // HAFTA SEÇİCİ
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { shift(-1L) }) { Text("◀", fontSize = 18.sp) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$year - Hafta $week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("${mon.format(dateFmt)} – ${mon.plusDays(4).format(dateFmt)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { shift(1L) }) { Text("▶", fontSize = 18.sp) }
            }
        }

        if (entries.isEmpty() && expenses.isEmpty()) {
            Text("Bu hafta için kayıt bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        // 1. TÜM ÇALIŞMA KAYITLARI TABLOSU (Yatay Kaydırılabilir)
        if (entries.isNotEmpty()) {
            Text("📋 Tüm Çalışma Kayıtları", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    // Tablo Başlıkları
                    Row(Modifier.background(MaterialTheme.colorScheme.primary)) {
                        TableCell("Gün", 100.dp, isHeader = true)
                        TableCell("Tarih", 110.dp, isHeader = true)
                        TableCell("Proje", 220.dp, isHeader = true)
                        TableCell("Çalışan", 100.dp, isHeader = true)
                        TableCell("Başlangıç", 90.dp, isHeader = true)
                        TableCell("Mola", 80.dp, isHeader = true)
                        TableCell("Bitiş", 80.dp, isHeader = true)
                        TableCell("Saat", 80.dp, isHeader = true)
                    }
                    
                    // Tablo Satırları
                    entries.sortedBy { it.day }.forEachIndexed { index, e ->
                        val bgColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        Row(Modifier.background(bgColor).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
                            TableCell(dayTr[e.day], 100.dp)
                            TableCell(mon.plusDays(e.day.toLong()).format(dateFmt), 110.dp)
                            TableCell(e.project, 220.dp)
                            TableCell(e.worker, 100.dp)
                            TableCell(hm(e.start), 90.dp)
                            TableCell(hm(e.pause), 80.dp)
                            TableCell(hm(e.end), 80.dp)
                            TableCell("${fmt(e.hours())} s", 80.dp, isBold = true)
                        }
                    }
                }
            }
        }

        // 2. ÇALIŞAN BAZLI TOPLAM SAATLER
        if (entries.isNotEmpty()) {
            Text("👷 Çalışan Bazlı Toplam Saatler", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            val workerHours = entries.groupBy { it.worker }.mapValues { (_, list) -> r2(list.sumOf { it.hours() }) }
            
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Column {
                    Row(Modifier.background(MaterialTheme.colorScheme.primary)) {
                        TableCell("Çalışan", 200.dp, isHeader = true)
                        TableCell("Saat", 100.dp, isHeader = true)
                    }
                    workerHours.forEach { (worker, hours) ->
                        Row(Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
                            TableCell(worker, 200.dp)
                            TableCell(fmt(hours), 100.dp, isBold = true)
                        }
                    }
                }
            }
        }

        // 3. PROJE BAZLI TOPLAM SAATLER
        if (entries.isNotEmpty()) {
            Text("📁 Proje Bazlı Toplam Saatler", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            val projHours = entries.groupBy { it.project }.mapValues { (_, list) -> r2(list.sumOf { it.hours() }) }
            
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Column {
                    Row(Modifier.background(MaterialTheme.colorScheme.primary)) {
                        TableCell("Proje", 300.dp, isHeader = true)
                        TableCell("Saat", 100.dp, isHeader = true)
                    }
                    projHours.forEach { (proj, hours) ->
                        Row(Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
                            TableCell(proj, 300.dp)
                            TableCell(fmt(hours), 100.dp, isBold = true)
                        }
                    }
                }
            }
        }

        // 4. PROJE & ÇALIŞAN MATRİSİ
        if (entries.isNotEmpty()) {
            Text("🔍 Proje & Çalışan Detay Matrisi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            
            val workers = entries.map { it.worker }.distinct().sorted()
            val projects = entries.map { it.project }.distinct().sorted()

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    Row(Modifier.background(MaterialTheme.colorScheme.primary)) {
                        TableCell("Çalışan", 120.dp, isHeader = true)
                        TableCell("Proje", 250.dp, isHeader = true)
                        workers.forEach { w -> TableCell(w, 100.dp, isHeader = true) }
                    }
                    projects.forEach { proj ->
                        Row(Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
                            TableCell("", 120.dp) // Boş bırakılan sol sütun (Görseldeki gibi)
                            TableCell(proj, 250.dp)
                            workers.forEach { worker ->
                                val h = entries.filter { it.worker == worker && it.project == proj }.sumOf { it.hours() }
                                TableCell(if (h > 0) fmt(r2(h)) else "0.0", 100.dp)
                            }
                        }
                    }
                }
            }
        }

        // 5. FİŞ / GİDER DETAYLARI
        if (expenses.isNotEmpty()) {
            Text("💶 Haftaya Ait Fiş ve Gider Detayları", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    expenses.sortedBy { it.day }.forEach { ex ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("🏷️ ${ex.category}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("📁 ${ex.project} | 📅 ${dayTr[ex.day]}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("€${"%.2f".format(ex.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    Spacer(Modifier.height(8.dp))
                    val totalCost = r2(expenses.sumOf { it.amount })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("Toplam Gider: ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("€$totalCost", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
    }
}

// YARDIMCI TABLO HÜCRESİ TASARIMI (Streamlit'teki gibi)
@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, isHeader: Boolean = false, isBold: Boolean = false) {
    Box(
        modifier = Modifier.width(width).padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = if (isHeader) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}
