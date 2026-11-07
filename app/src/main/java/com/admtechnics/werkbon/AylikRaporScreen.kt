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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AylikRaporScreen(state: AppState) {
    val ctx = LocalContext.current
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    var selectedYear by remember { mutableStateOf(WeekNav.year) }
    var selectedMonth by remember { mutableStateOf(LocalDate.now().monthValue) }
    var selectedWorkerFilter by remember { mutableStateOf("Tümü") }

    val months = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
    val years = (2024..2030).toList()
    val workersList = listOf("Tümü") + state.workers.map { it.name }

    val weeksData = WeekStore.load(ctx)
    val expensesData = ExpenseStore.load(ctx)

    // Aylık verileri filtrele
    val monthlyEntries = mutableListOf<WorkEntry>()
    val monthlyExpenses = mutableListOf<Expense>()

    for ((key, entries) in weeksData) {
        // key formatı: "2026-W45"
        try {
            val y = key.substringBefore("-W").toInt()
            val w = key.substringAfter("-W").toInt()
            val mon = mondayOf(y, w)
            
            entries.forEach { e ->
                val entryDate = mon.plusDays(e.day.toLong())
                if (entryDate.year == selectedYear && entryDate.monthValue == selectedMonth) {
                    if (selectedWorkerFilter == "Tümü" || e.worker == selectedWorkerFilter) {
                        monthlyEntries.add(e)
                    }
                }
            }
        } catch (e: Exception) {}
    }

    for ((key, exps) in expensesData) {
        try {
            val y = key.substringBefore("-W").toInt()
            val w = key.substringAfter("-W").toInt()
            val mon = mondayOf(y, w)
            
            exps.forEach { ex ->
                val expDate = mon.plusDays(ex.day.toLong())
                if (expDate.year == selectedYear && expDate.monthValue == selectedMonth) {
                    monthlyExpenses.add(ex)
                }
            }
        } catch (e: Exception) {}
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        
        // FİLTRELER KARTI
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Filtreler", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                
                Picker("Yıl", selectedYear.toString(), years.map { it.toString() }, { selectedYear = it.toInt() }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Picker("Ay", months[selectedMonth - 1], months, { selectedMonth = months.indexOf(it) + 1 }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Picker("Çalışan", selectedWorkerFilter, workersList, { selectedWorkerFilter = it }, Modifier.fillMaxWidth())
            }
        }

        // KAZANÇ TABLOSU
        if (monthlyEntries.isNotEmpty()) {
            Text("💶 Çalışan Bazlı Aylık Kazanç Hesaplama", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            
            val workerHoursMap = monthlyEntries.groupBy { it.worker }.mapValues { r2(it.value.sumOf { e -> e.hours() }) }
            var totalEarnings = 0.0

            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Column {
                    Row(Modifier.background(TableHeaderColor)) {
                        TableHeader("Çalışan", 100.dp)
                        TableHeader("Toplam Saat", 100.dp)
                        TableHeader("Saatlik Ücret (€)", 120.dp)
                        TableHeader("Toplam Kazanç (€)", 120.dp)
                    }
                    workerHoursMap.forEach { (worker, hours) ->
                        val rate = state.workers.find { it.name == worker }?.rate ?: 0.0
                        val earnings = r2(hours * rate)
                        totalEarnings += earnings

                        Row(Modifier.background(MaterialTheme.colorScheme.surface)) {
                            TableCell(worker, 100.dp, FontWeight.Medium)
                            TableCell("$hours", 100.dp)
                            TableCell("€${rate}", 120.dp)
                            TableCell("€${earnings}", 120.dp, FontWeight.Bold)
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
            Text("Seçilen Dönem Toplam İşçi Maliyeti / Kazancı: €$totalEarnings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(24.dp))
        }

        // ÖZET METRİKLERİ
        val totalHours = r2(monthlyEntries.sumOf { it.hours() })
        val totalExpenses = r2(monthlyExpenses.sumOf { it.amount })
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("📌 ${months[selectedMonth - 1]} $selectedYear Toplam Çalışma Saati", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$totalHours Saat", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Spacer(Modifier.height(12.dp))
                
                Text("💶 ${months[selectedMonth - 1]} $selectedYear Toplam Gider / Malzeme", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("€$totalExpenses", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
        }

        // ÇALIŞAN BAZLI TABLO
        if (monthlyEntries.isNotEmpty()) {
            Text("👷 Çalışan Bazlı Aylık Toplamlar", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            val workerHoursMap = monthlyEntries.groupBy { it.worker }.mapValues { r2(it.value.sumOf { e -> e.hours() }) }
            
            Box(Modifier.fillMaxWidth()) {
                Column {
                    Row(Modifier.background(TableHeaderColor)) {
                        TableHeader("Çalışan", 180.dp)
                        TableHeader("Saat", 100.dp)
                    }
                    workerHoursMap.forEach { (worker, hours) ->
                        Row(Modifier.background(MaterialTheme.colorScheme.surface)) {
                            TableCell(worker, 180.dp, FontWeight.Medium)
                            TableCell("$hours", 100.dp)
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // PROJE BAZLI TABLO
            Text("📁 Proje Bazlı Aylık Toplamlar", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
            val projHoursMap = monthlyEntries.groupBy { it.project }.mapValues { r2(it.value.sumOf { e -> e.hours() }) }
            
            Box(Modifier.fillMaxWidth()) {
                Column {
                    Row(Modifier.background(TableHeaderColor)) {
                        TableHeader("Proje", 280.dp)
                        TableHeader("Saat", 100.dp)
                    }
                    projHoursMap.forEach { (proj, hours) ->
                        Row(Modifier.background(MaterialTheme.colorScheme.surface)) {
                            TableCell(proj, 280.dp)
                            TableCell("$hours", 100.dp)
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
