package com.admtechnics.werkbon

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.IsoFields

data class WorkEntry(
    val day: Int,
    val project: String,
    val worker: String,
    val start: Int,
    val pause: Int,
    val end: Int
) {
    fun hours(): Double {
        val net = end - start - pause
        return if (net <= 0) 0.0 else r2(net / 60.0)
    }
}

object WeekNav {
    var year: Int = LocalDate.now().get(IsoFields.WEEK_BASED_YEAR)
    var week: Int = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
}

object WeekStore {
    private const val WEEKS_FILE = "werkbon_weeks.json"

    fun load(ctx: Context): Map<String, List<WorkEntry>> {
        val f = File(ctx.filesDir, WEEKS_FILE)
        if (!f.exists()) return emptyMap()
        return try {
            val o = JSONObject(f.readText())
            val out = LinkedHashMap<String, List<WorkEntry>>()
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = o.getJSONArray(k)
                val list = ArrayList<WorkEntry>()
                for (i in 0 until arr.length()) {
                    val x = arr.getJSONObject(i)
                    list.add(
                        WorkEntry(
                            x.getInt("day"),
                            x.getString("project"),
                            x.getString("worker"),
                            x.getInt("start"),
                            x.getInt("pause"),
                            x.getInt("end")
                        )
                    )
                }
                out[k] = list
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun save(ctx: Context, weeks: Map<String, List<WorkEntry>>) {
        val o = JSONObject()
        for ((k, list) in weeks) {
            val arr = JSONArray()
            for (e in list) {
                arr.put(
                    JSONObject()
                        .put("day", e.day)
                        .put("project", e.project)
                        .put("worker", e.worker)
                        .put("start", e.start)
                        .put("pause", e.pause)
                        .put("end", e.end)
                )
            }
            o.put(k, arr)
        }
        File(ctx.filesDir, WEEKS_FILE).writeText(o.toString())
    }
}

val dayTr = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma")
val dayNl = listOf("Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrijdag")

fun r2(x: Double): Double = Math.round(x * 100.0) / 100.0

fun hm(m: Int): String = "%02d:%02d".format(m / 60, m % 60)

fun defaultTimes(worker: String): List<Int> =
    if (worker == "Adem") listOf(330, 105, 1035) else listOf(420, 45, 945)

fun mondayOf(year: Int, week: Int): LocalDate =
    LocalDate.of(year, 1, 4)
        .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week.toLong())
        .with(ChronoField.DAY_OF_WEEK, 1L)

fun pickTime(ctx: Context, minutes: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(
        ctx,
        { _, h, m -> onPicked(h * 60 + m) },
        minutes / 60,
        minutes % 60,
        true
    ).show()
}

@Composable
fun Picker(
    label: String,
    value: String,
    options: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: $value", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o) },
                    onClick = {
                        onPick(o)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
fun TimeButton(label: String, minutes: Int, onPicked: (Int) -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = { pickTime(ctx, minutes, onPicked) },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp)
            Text(hm(minutes), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EntryCard(
    e: WorkEntry,
    projects: List<String>,
    workers: List<String>,
    onChange: (WorkEntry) -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(8.dp)) {
            Picker("Proje", e.project, projects, { p -> onChange(e.copy(project = p)) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Picker("Çalışan", e.worker, workers, { w ->
                val t = defaultTimes(w)
                onChange(e.copy(worker = w, start = t[0], pause = t[1], end = t[2]))
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TimeButton("Başlangıç", e.start, { v -> onChange(e.copy(start = v)) }, Modifier.weight(1f))
                TimeButton("Mola", e.pause, { v -> onChange(e.copy(pause = v)) }, Modifier.weight(1f))
                TimeButton("Bitiş", e.end, { v -> onChange(e.copy(end = v)) }, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Net: ${fmt(e.hours())} saat", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                TextButton(onClick = onDelete) { Text("Sil") }
            }
        }
    }
}

@Composable
fun WerkbonScreen(state: AppState) {
    val ctx = LocalContext.current
    var year by remember { mutableStateOf(WeekNav.year) }
    var week by remember { mutableStateOf(WeekNav.week) }
    var weeks by remember { mutableStateOf(WeekStore.load(ctx)) }

    val key = "$year-W$week"
    val entries = weeks[key] ?: emptyList()
    val projects = state.projects.sortedByDescending { it.code }.map { "${it.code} - ${it.name}" }
    val workers = state.workers.map { it.name }
    val mon = mondayOf(year, week)
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun setEntries(list: List<WorkEntry>) {
        val m = LinkedHashMap(weeks)
        m[key] = list
        weeks = m
        WeekStore.save(ctx, m)
    }

    fun shift(delta: Long) {
        val m = mondayOf(year, week).plusWeeks(delta)
        year = m.get(IsoFields.WEEK_BASED_YEAR)
        week = m.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        WeekNav.year = year
        WeekNav.week = week
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { shift(-1L) }) { Text("◀") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$year - Hafta $week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("${mon.format(dateFmt)} – ${mon.plusDays(4).format(dateFmt)}")
            }
            TextButton(onClick = { shift(1L) }) { Text("▶") }
        }

        val total = r2(entries.map { it.hours() }.sum())
        Text("Hafta toplamı: ${fmt(total)} saat", fontWeight = FontWeight.Bold)

        if (projects.isEmpty() || workers.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Önce Projeler ve Ayarlar sekmelerinden en az bir proje ve bir çalışan ekleyin.")
        }

        for (d in 0..4) {
            val dayEntries = entries.withIndex().filter { it.value.day == d }
            val date = mon.plusDays(d.toLong())
            val dayTotal = r2(dayEntries.map { it.value.hours() }.sum())
            Spacer(Modifier.height(14.dp))
            Text("${dayTr[d]} (${dayNl[d]}) - ${date.format(dateFmt)}", fontWeight = FontWeight.Bold)
            if (dayEntries.isNotEmpty()) {
                Text("Gün toplamı: ${fmt(dayTotal)} saat")
            }
            for (iv in dayEntries) {
                val idx = iv.index
                EntryCard(
                    e = iv.value,
                    projects = projects,
                    workers = workers,
                    onChange = { ne ->
                        setEntries(entries.toMutableList().also { l -> l[idx] = ne })
                    },
                    onDelete = {
                        setEntries(entries.toMutableList().also { l -> l.removeAt(idx) })
                    }
                )
            }
            if (projects.isNotEmpty() && workers.isNotEmpty()) {
                TextButton(onClick = {
                    val w = workers[0]
                    val t = defaultTimes(w)
                    setEntries(entries + WorkEntry(d, projects[0], w, t[0], t[1], t[2]))
                }) { Text("+ Kayıt ekle") }
            }
        }

        // ====== PDF OLUŞTURMA BÖLÜMÜ ======
        Spacer(Modifier.height(20.dp))
        Text("Werkbon PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        val pdfItems = projects.filter { pLabel ->
            val hasEntries = entries.any { it.project == pLabel }
            val hasExpenses = ExpenseStore.load(ctx)[key]?.any { it.project == pLabel } == true
            hasEntries || hasExpenses
        }

        if (pdfItems.isEmpty()) {
            Text("Bu hafta için kayıtlı proje yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            pdfItems.forEach { pLabel ->
                OutlinedButton(
                    onClick = {
                        PdfMaker.admContextHolder = ctx
                        val expList = ExpenseStore.load(ctx)[key] ?: emptyList()
                        val result = PdfMaker.makePdf(
                            ctx = ctx,
                            state = state,
                            projectLabel = pLabel,
                            weekEntries = entries,
                            weekExpenses = expList,
                            year = year,
                            week = week
                        )
                        sharePdf(ctx, result.file)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📄 PDF: $pLabel")
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        // ====== PDF BÖLÜMÜ SONU ======

        ExpensesSection(state, key)
        Spacer(Modifier.height(24.dp))
    }
}

fun sharePdf(ctx: android.content.Context, file: java.io.File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(
            android.content.Intent.createChooser(intent, "PDF Paylaş")
        )
    } catch (e: Exception) {
        android.widget.Toast.makeText(ctx, "Hata: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
