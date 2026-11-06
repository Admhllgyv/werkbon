package com.admtechnics.werkbon

import androidx.compose.foundation.background
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
        
        // HAFTA SEÇİCİ KARTI
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { shift(-1L) }) { Text("◀", fontSize = 18.sp) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$year - Hafta $week",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${mon.format(dateFmt)} – ${mon.plusDays(4).format(dateFmt)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { shift(1L) }) { Text("▶", fontSize = 18.sp) }
            }
        }

        if (entries.isEmpty() && expenses.isEmpty()) {
            Text("Bu hafta için kayıt bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        // ÇALIŞMA KAYITLARI TABLOSU
        if (entries.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Çalışma Kayıtları", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))

                    // Tablo Başlıkları
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).padding(8.dp)) {
                        Text("Gün", Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Proje", Modifier.weight(1.5f), color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Çalışan", Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Saat", Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Tablo Satırları
                    entries.sortedBy { it.day }.forEachIndexed { index, e ->
                        val bgColor = if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        Row(Modifier.fillMaxWidth().background(bgColor).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(dayTr[e.day], Modifier.weight(1.2f), fontSize = 11.sp)
                            Text(e.project.substringAfter("- "), Modifier.weight(1.5f), fontSize = 11.sp, maxLines = 1)
                            Text(e.worker, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("${fmt(e.hours())} s", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    }
                }
            }
        }

        // ÇALIŞAN BAZLI ÖZET
        if (entries.isNotEmpty()) {
            val workerHours = entries.groupBy { it.worker }.mapValues { (_, list) -> r2(list.sumOf { it.hours() }) }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Çalışan Bazlı Toplam Saat", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    workerHours.forEach { (worker, hours) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("• $worker", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Text("$hours Saat", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // FİŞ / GİDER ÖZETİ
        if (expenses.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Fiş / Gider Detayları", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))

                    expenses.sortedBy { it.day }.forEach { ex ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${ex.category} (${dayTr[ex.day]})", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text(ex.project.substringAfter("- "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("€${ex.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    val totalCost = r2(expenses.sumOf { it.amount })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("Toplam Gider: ", fontWeight = FontWeight.Bold)
                        Text("€$totalCost", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
    }
}
