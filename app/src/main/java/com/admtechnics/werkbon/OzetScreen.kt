package com.admtechnics.werkbon

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import com.admtechnics.werkbon.ui.theme.TableHeaderColor
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
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { shift(-1L) }) { Text("◀ Önceki Hafta", fontSize = 12.sp) }
            Text("$year - Hafta $week", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            TextButton(onClick = { shift(1L) }) { Text("Sonraki Hafta ▶", fontSize = 12.sp) }
        }

        if (entries.isEmpty() && expenses.isEmpty()) {
            Text("Bu hafta için kayıt bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        // ÇALIŞMA KAYITLARI TABLOSU
        if (entries.isNotEmpty()) {
            Text("📋 Tüm Çalışma Kayıtları", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            
            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Column {
                    // Başlık
                    Row(Modifier.background(TableHeaderColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))) {
                        TableHeader("Gün", 100.dp)
                        TableHeader("Tarih", 100.dp)
                        TableHeader("Proje", 200.dp)
                        TableHeader("Çalışan", 100.dp)
                        TableHeader("Saat", 80.dp)
                    }
                    // Satırlar
                    entries.sortedBy { it.day }.forEach { e ->
                        Row(Modifier.background(MaterialTheme.colorScheme.surface)) {
                            TableCell(dayTr[e.day], 100.dp)
                            TableCell(mon.plusDays(e.day.toLong()).format(dateFmt), 100.dp)
                            TableCell(e.project, 200.dp)
                            TableCell(e.worker, 100.dp)
                            TableCell("${fmt(e.hours())}", 80.dp)
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ÇALIŞAN BAZLI TABLO
        if (entries.isNotEmpty()) {
            Text("👷 Çalışan Bazlı Toplam Saatler", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            val workerHours = entries.groupBy { it.worker }.mapValues { r2(it.value.sumOf { e -> e.hours() }) }
            
            Box(Modifier.fillMaxWidth()) {
                Column {
                    Row(Modifier.background(TableHeaderColor)) {
                        TableHeader("Çalışan", 180.dp)
                        TableHeader("Saat", 100.dp)
                    }
                    workerHours.forEach { (worker, hours) ->
                        Row(Modifier.background(MaterialTheme.colorScheme.surface)) {
                            TableCell(worker, 180.dp, FontWeight.Medium)
                            TableCell("$hours", 100.dp)
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // PROJE BAZLI TABLO
        if (entries.isNotEmpty()) {
            Text("📁 Proje Bazlı Toplam Saatler", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            val projHours = entries.groupBy { it.project }.mapValues { r2(it.value.sumOf { e -> e.hours() }) }
            
            Box(Modifier.fillMaxWidth()) {
                Column {
                    Row(Modifier.background(TableHeaderColor)) {
                        TableHeader("Proje", 280.dp)
                        TableHeader("Saat", 100.dp)
                    }
                    projHours.forEach { (proj, hours) ->
                        Row(Modifier.background(MaterialTheme.colorScheme.surface)) {
                            TableCell(proj, 280.dp)
                            TableCell("$hours", 100.dp)
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // FİŞ DETAYLARI
        if (expenses.isNotEmpty()) {
            Text("💶 Fiş / Gider Detayları", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            expenses.forEach { ex ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${ex.category} (${dayTr[ex.day]})", fontWeight = FontWeight.Medium)
                        Text(ex.project.substringAfter("- "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("€${ex.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Divider()
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun TableHeader(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(8.dp),
        color = androidx.compose.ui.graphics.Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
}

@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, weight: FontWeight = FontWeight.Normal) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(12.dp),
        fontSize = 12.sp,
        fontWeight = weight,
        maxLines = 1
    )
}
