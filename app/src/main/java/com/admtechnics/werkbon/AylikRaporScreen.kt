package com.admtechnics.werkbon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import java.time.LocalDate

@Composable
fun AylikRaporScreen(state: AppState, title: String) {
    val ctx = LocalContext.current

    var selectedYear by remember { mutableStateOf(WeekNav.year) }
    var selectedMonth by remember { mutableStateOf(LocalDate.now().monthValue) }
    var selectedWorkerFilter by remember { mutableStateOf("Tümü") }

    val months = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
    val years = (2024..2030).toList()
    val workersList = listOf("Tümü") + state.workers.map { it.name }

    val weeksData = WeekStore.load(ctx)
    val expensesData = ExpenseStore.load(ctx)

    val monthlyEntries = mutableListOf<WorkEntry>()
    val monthlyExpenses = mutableListOf<Expense>()

    for ((key, entries) in weeksData) {
        try {
            val y = key.substringBefore("-W").toInt(); val w = key.substringAfter("-W").toInt(); val mon = mondayOf(y, w)
            entries.forEach { e -> val entryDate = mon.plusDays(e.day.toLong()); if (entryDate.year == selectedYear && entryDate.monthValue == selectedMonth) { if (selectedWorkerFilter == "Tümü" || e.worker == selectedWorkerFilter) { monthlyEntries.add(e) } } }
        } catch (e: Exception) {}
    }

    for ((key, exps) in expensesData) {
        try {
            val y = key.substringBefore("-W").toInt(); val w = key.substringAfter("-W").toInt(); val mon = mondayOf(y, w)
            exps.forEach { ex -> val expDate = mon.plusDays(ex.day.toLong()); if (expDate.year == selectedYear && expDate.monthValue == selectedMonth) { monthlyExpenses.add(ex) } }
        } catch (e: Exception) {}
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        TopHeaderCard(title)
        
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Filtreler", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Picker("Yıl", selectedYear.toString(), years.map { it.toString() }, { selectedYear = it.toInt() }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Picker("Ay", months[selectedMonth - 1], months, { selectedMonth = months.indexOf(it) + 1 }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Picker("Çalışan", selectedWorkerFilter, workersList, { selectedWorkerFilter = it }, Modifier.fillMaxWidth())
            }
        }

        if (monthlyEntries.isNotEmpty()) {
            Text("💶 Aylık Kazanç Hesaplama", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 8.dp))
            
            val workerHoursMap = monthlyEntries.groupBy { it.worker }.mapValues { r2(it.value.sumOf { e -> e.hours() }) }
            var totalEarnings = 0.0

            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))) {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(vertical = 8.dp)) {
                    TableHeader("Çalışan", 1f); TableHeader("Saat", 1f); TableHeader("Ücret", 1f); TableHeader("Kazanç", 1.2f)
                }
                workerHoursMap.forEach { (worker, hours) ->
                    val rate = state.workers.find { it.name == worker }?.rate ?: 0.0
                    val earnings = r2(hours * rate); totalEarnings += earnings

                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TableCell(worker, 1f, FontWeight.Medium); TableCell("$hours", 1f); TableCell("€${rate}", 1f); TableCell("€${earnings}", 1.2f, FontWeight.Bold, MaterialTheme.colorScheme.primary)
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
            Text("Seçilen Dönem Toplam Kazanç: €$totalEarnings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(24.dp))
        }

        val totalHours = r2(monthlyEntries.sumOf { it.hours() })
        val totalExpenses = r2(monthlyExpenses.sumOf { it.amount })
        
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("📌 ${months[selectedMonth - 1]} $selectedYear Toplam Çalışma Saati", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$totalHours Saat", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("💶 ${months[selectedMonth - 1]} $selectedYear Toplam Gider / Malzeme", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("€$totalExpenses", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
        }

        if (monthlyEntries.isNotEmpty()) {
            Text("👷 Çalışan Bazlı Aylık Toplamlar", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 8.dp))
            val workerHoursMap = monthlyEntries.groupBy { it.worker }.mapValues { r2(it.value.sumOf { e -> e.hours() }) }
            
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))) {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(vertical = 8.dp)) { TableHeader("Çalışan", 1f); TableHeader("Saat", 1f) }
                workerHoursMap.forEach { (worker, hours) -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { TableCell(worker, 1f, FontWeight.Medium); TableCell("$hours", 1f) }; Divider(color = Color.LightGray.copy(alpha = 0.5f)) }
            }
            Spacer(Modifier.height(24.dp))

            Text("📁 Proje Bazlı Aylık Toplamlar", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 8.dp))
            val projHoursMap = monthlyEntries.groupBy { it.project }.mapValues { r2(it.value.sumOf { e -> e.hours() }) }
            
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))) {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(vertical = 8.dp)) { TableHeader("Proje", 1.5f); TableHeader("Saat", 1f) }
                projHoursMap.forEach { (proj, hours) -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { TableCell(proj, 1.5f); TableCell("$hours", 1f) }; Divider(color = Color.LightGray.copy(alpha = 0.5f)) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun RowScope.TableHeader(text: String, weight: Float) {
    Text(text = text, modifier = Modifier.weight(weight).padding(horizontal = 4.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
}

@Composable
fun RowScope.TableCell(text: String, weight: Float, fontWeight: FontWeight = FontWeight.Normal, color: Color = Color.Black) {
    Text(text = text, modifier = Modifier.weight(weight).padding(horizontal = 4.dp), fontSize = 12.sp, fontWeight = fontWeight, color = color, maxLines = 1)
}
