package com.admtechnics.werkbon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

data class DatedEntry(val date: LocalDate, val e: WorkEntry)

val monthNames = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
)

fun allDated(weeks: Map<String, List<WorkEntry>>): List<DatedEntry> {
    val out = ArrayList<DatedEntry>()
    for ((k, list) in weeks) {
        val parts = k.split("-W")
        if (parts.size != 2) continue
        val y = parts[0].toIntOrNull() ?: continue
        val w = parts[1].toIntOrNull() ?: continue
        val mon = try {
            mondayOf(y, w)
        } catch (ex: Exception) {
            continue
        }
        for (e in list) {
            out.add(DatedEntry(mon.plusDays(e.day.toLong()), e))
        }
    }
    return out
}

fun sumHours(list: List<WorkEntry>): Double = r2(list.map { it.hours() }.sum())

fun hoursBy(entries: List<WorkEntry>, key: (WorkEntry) -> String): List<Pair<String, Double>> =
    entries.groupBy(key).map { (k, v) -> k to sumHours(v) }.sortedBy { it.first }

@Composable
fun InfoCard(title: String, lines: List<Pair<String, String>>) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            if (lines.isEmpty()) {
                Text("Kayıt yok")
            }
            for (l in lines) {
                Row(Modifier.fillMaxWidth()) {
                    Text(l.first, Modifier.weight(1f))
                    Text(l.second, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun OzetScreen(state: AppState) {
    val ctx = LocalContext.current
    val weeks = remember { WeekStore.load(ctx) }
    var mode by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (mode == 0) {
                Button(onClick = { mode = 0 }, modifier = Modifier.weight(1f)) { Text("Haftalık") }
            } else {
                OutlinedButton(onClick = { mode = 0 }, modifier = Modifier.weight(1f)) { Text("Haftalık") }
            }
            if (mode == 1) {
                Button(onClick = { mode = 1 }, modifier = Modifier.weight(1f)) { Text("Aylık") }
            } else {
                OutlinedButton(onClick = { mode = 1 }, modifier = Modifier.weight(1f)) { Text("Aylık") }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (mode == 0) {
            WeeklyView(weeks)
        } else {
            MonthlyView(state, weeks)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun WeeklyView(weeks: Map<String, List<WorkEntry>>) {
    var year by remember { mutableStateOf(WeekNav.year) }
    var week by remember { mutableStateOf(WeekNav.week) }
    val entries = weeks["$year-W$week"] ?: emptyList()
    val mon = mondayOf(year, week)
    val df = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun shift(delta: Long) {
        val m = mondayOf(year, week).plusWeeks(delta)
        year = m.get(IsoFields.WEEK_BASED_YEAR)
        week = m.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        WeekNav.year = year
        WeekNav.week = week
    }

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { shift(-1L) }) { Text("◀") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$year - Hafta $week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("${mon.format(df)} – ${mon.plusDays(4).format(df)}")
            }
            TextButton(onClick = { shift(1L) }) { Text("▶") }
        }

        if (entries.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Bu hafta için kayıt yok.")
        } else {
            Text("Hafta toplamı: ${fmt(sumHours(entries))} saat", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            val byWorker = hoursBy(entries) { it.worker }.map { it.first to "${fmt(it.second)} saat" }
            InfoCard("Çalışan bazlı toplam", byWorker)

            val byProject = hoursBy(entries) { it.project }.map { it.first to "${fmt(it.second)} saat" }
            InfoCard("Proje bazlı toplam", byProject)

            Title("Kayıtlar")
            for (d in 0..4) {
                val de = entries.filter { it.day == d }
                if (de.isEmpty()) continue
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "${dayTr[d]} (${dayNl[d]}) - ${mon.plusDays(d.toLong()).format(df)}",
                            fontWeight = FontWeight.Bold
                        )
                        for (e in de) {
                            Spacer(Modifier.height(6.dp))
                            Text("${e.worker} • ${e.project}")
                            Text("${hm(e.start)}–${hm(e.end)}, mola ${hm(e.pause)} → ${fmt(e.hours())} saat")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyView(state: AppState, weeks: Map<String, List<WorkEntry>>) {
    var my by remember { mutableStateOf(LocalDate.now().year) }
    var mm by remember { mutableStateOf(LocalDate.now().monthValue) }
    var filter by remember { mutableStateOf("Tümü") }

    val dated = allDated(weeks)
    val list = dated
        .filter { it.date.year == my && it.date.monthValue == mm }
        .filter { filter == "Tümü" || it.e.worker == filter }
        .map { it.e }

    fun shift(delta: Long) {
        val d = LocalDate.of(my, mm, 1).plusMonths(delta)
        my = d.year
        mm = d.monthValue
    }

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { shift(-1L) }) { Text("◀") }
            Text(
                "${monthNames[mm - 1]} $my",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { shift(1L) }) { Text("▶") }
        }

        Picker(
            "Çalışan",
            filter,
            listOf("Tümü") + state.workers.map { it.name },
            { v -> filter = v },
            Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        if (list.isEmpty()) {
            Text("Bu dönem için kayıt yok.")
        } else {
            Text("Toplam çalışma: ${fmt(sumHours(list))} saat", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            val byWorker = hoursBy(list) { it.worker }
            val earningLines = ArrayList<Pair<String, String>>()
            var totalEarn = 0.0
            for ((n, h) in byWorker) {
                val rate = state.workers.firstOrNull { it.name == n }?.rate ?: 0.0
                val earn = r2(h * rate)
                totalEarn += earn
                val earnTxt = "%.2f".format(earn)
                earningLines.add(n to "${fmt(h)} saat × €${fmt(rate)} = €$earnTxt")
            }
            val totalTxt = "%.2f".format(totalEarn)
            earningLines.add("Toplam işçi maliyeti" to "€$totalTxt")
            InfoCard("Çalışan bazlı kazanç", earningLines)

            val byProject = hoursBy(list) { it.project }.map { it.first to "${fmt(it.second)} saat" }
            InfoCard("Proje bazlı toplam", byProject)

            val matrix = ArrayList<Pair<String, String>>()
            val groups = list.groupBy { it.project }.toList().sortedBy { it.first }
            for ((p, ents) in groups) {
                matrix.add(p to "${fmt(sumHours(ents))} saat")
                val sub = hoursBy(ents) { it.worker }
                for ((w, h) in sub) {
                    matrix.add("    $w" to fmt(h))
                }
            }
            InfoCard("Proje ve çalışan detayı", matrix)
        }
    }
}
