package com.admtechnics.werkbon

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.IsoFields

data class WorkEntry(
    val day: Int, val project: String, val worker: String, val start: Int, val pause: Int, val end: Int
) { fun hours(): Double { val net = end - start - pause; return if (net <= 0) 0.0 else r2(net / 60.0) } }
object WeekNav { var year: Int = LocalDate.now().get(IsoFields.WEEK_BASED_YEAR); var week: Int = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) }
object WeekStore {
    private const val WEEKS_FILE = "werkbon_weeks.json"
    fun load(ctx: Context): Map<String, List<WorkEntry>> {
        val f = File(ctx.filesDir, WEEKS_FILE); if (!f.exists()) return emptyMap()
        return try {
            val o = JSONObject(f.readText()); val out = LinkedHashMap<String, List<WorkEntry>>(); val keys = o.keys()
            while (keys.hasNext()) { val k = keys.next(); val arr = o.getJSONArray(k); val list = ArrayList<WorkEntry>()
                for (i in 0 until arr.length()) { val x = arr.getJSONObject(i); list.add(WorkEntry(x.getInt("day"), x.getString("project"), x.getString("worker"), x.getInt("start"), x.getInt("pause"), x.getInt("end"))) }
                out[k] = list }
            out } catch (e: Exception) { emptyMap() }
    }
    fun save(ctx: Context, weeks: Map<String, List<WorkEntry>>) {
        val o = JSONObject(); for ((k, list) in weeks) { val arr = JSONArray()
            for (e in list) { arr.put(JSONObject().put("day", e.day).put("project", e.project).put("worker", e.worker).put("start", e.start).put("pause", e.pause).put("end", e.end)) }
            o.put(k, arr) }
        File(ctx.filesDir, WEEKS_FILE).writeText(o.toString())
    }
}
val dayTr = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma")
val dayNl = listOf("Maandag", "Dinsdag", "Woensdag", "Donderdag", "Vrijdag")
fun r2(x: Double): Double = Math.round(x * 100.0) / 100.0
fun hm(m: Int): String = "%02d:%02d".format(m / 60, m % 60)
fun defaultTimes(worker: String): List<Int> = if (worker == "Adem") listOf(330, 105, 1035) else listOf(420, 45, 945)
fun mondayOf(year: Int, week: Int): LocalDate = LocalDate.of(year, 1, 4).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week.toLong()).with(ChronoField.DAY_OF_WEEK, 1L)
fun pickTime(ctx: Context, minutes: Int, onPicked: (Int) -> Unit) { TimePickerDialog(ctx, { _, h, m -> onPicked(h * 60 + m) }, minutes / 60, minutes % 60, true).show() }

@Composable
fun Picker(label: String, value: String, options: List<String>, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("$label: $value", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o, fontSize = 13.sp) }, onClick = { onPick(o); open = false }) }
        }
    }
}

@Composable
fun TimeButton(label: String, minutes: Int, onPicked: (Int) -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    OutlinedButton(onClick = { pickTime(ctx, minutes, onPicked) }, modifier = modifier.height(44.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(hm(minutes), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
fun EntryCard(e: WorkEntry, projects: List<String>, workers: List<String>, onChange: (WorkEntry) -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Picker("Proje", e.project, projects, { p -> onChange(e.copy(project = p)) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Picker("Çalışan", e.worker, workers, { w -> onChange(e.copy(worker = w, start = defaultTimes(w)[0], pause = defaultTimes(w)[1], end = defaultTimes(w)[2])) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimeButton("Başlangıç", e.start, { v -> onChange(e.copy(start = v)) }, Modifier.weight(1f))
                TimeButton("Mola", e.pause, { v -> onChange(e.copy(pause = v)) }, Modifier.weight(1f))
                TimeButton("Bitiş", e.end, { v -> onChange(e.copy(end = v)) }, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top=8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Net: ${fmt(e.hours())} saat", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onDelete, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(0.dp)) { Text("Sil", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
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
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun setEntries(list: List<WorkEntry>) { val m = LinkedHashMap(weeks); m[key] = list; weeks = m; WeekStore.save(ctx, m) }
    fun shift(delta: Long) { val m = mondayOf(year, week).plusWeeks(delta); year = m.get(IsoFields.WEEK_BASED_YEAR); week = m.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR); WeekNav.year = year; WeekNav.week = week }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.LightGray),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { shift(-1L) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp)) { Text("◀", fontSize = 14.sp) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$year - Hafta $week", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Text("${mondayOf(year, week).format(dateFmt)} – ${mondayOf(year, week).plusDays(4).format(dateFmt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { shift(1L) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp)) { Text("▶", fontSize = 14.sp) }
            }
        }

        val total = r2(entries.map { it.hours() }.sum())
        Text("Hafta toplamı: ${fmt(total)} saat", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))

        for (d in 0..4) {
            val dayEntries = entries.withIndex().filter { it.value.day == d }
            val date = mondayOf(year, week).plusDays(d.toLong())
            val dayTotal = r2(dayEntries.map { it.value.hours() }.sum())
            
            var isExpanded by remember { mutableStateOf(false) }
            val rotationState by animateFloatAsState(if (isExpanded) 180f else 0f)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if(isExpanded) MaterialTheme.colorScheme.primary else Color.LightGray),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column {
                    Row(Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${dayTr[d]} (${dayNl[d]})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(date.format(dateFmt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (dayEntries.isNotEmpty()) {
                            Text("${fmt(dayTotal)} sa", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                        }
                        Icon(Icons.Default.KeyboardArrowDown, "Aç", modifier = Modifier.size(20.dp).rotate(rotationState))
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                            Divider(color = Color.LightGray.copy(alpha=0.5f))
                            Spacer(Modifier.height(8.dp))
                            
                            if (dayEntries.isEmpty()) {
                                Text("Bu gün için kayıt yok.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                            } else {
                                for (iv in dayEntries) {
                                    val idx = iv.index
                                    EntryCard(
                                        e = iv.value, projects = projects, workers = workers,
                                        onChange = { ne -> setEntries(entries.toMutableList().also { l -> l[idx] = ne }) },
                                        onDelete = { setEntries(entries.toMutableList().also { l -> l.removeAt(idx) }) }
                                    )
                                }
                            }
                            
                            if (projects.isNotEmpty() && workers.isNotEmpty()) {
                                TextButton(onClick = {
                                    val w = workers[0]; val t = defaultTimes(w)
                                    setEntries(entries + WorkEntry(d, projects[0], w, t[0], t[1], t[2]))
                                }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) { 
                                    Text("+ Kayıt ekle", fontSize = 12.sp) 
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("💶 Fiş / Gider", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            ExpensesSection(state, key, day = d)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Werkbon PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)

        val pdfItems = projects.filter { pLabel -> entries.any { it.project == pLabel } || ExpenseStore.load(ctx)[key]?.any { it.project == pLabel } == true }

        if (pdfItems.isEmpty()) {
            Text("Bu hafta için kayıtlı proje yok.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            pdfItems.forEach { pLabel ->
                OutlinedButton(
                    onClick = {
                        PdfMaker.admContextHolder = ctx
                        val expList = ExpenseStore.load(ctx)[key] ?: emptyList()
                        val result = PdfMaker.makePdf(ctx, state, pLabel, entries, expList, year, week)
                        showPdfOptions(ctx, result.file)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("📄 PDF: $pLabel", fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// EKSİK OLAN PDF İŞLEM FONKSİYONLARI BURAYA EKLENDİ
fun showPdfOptions(ctx: Context, file: File) {
    val options = arrayOf("👁️ Görüntüle", "📥 İndir (Kaydet)", "📤 Paylaş")
    AlertDialog.Builder(ctx)
        .setTitle("PDF İşlemleri")
        .setItems(options) { _, which ->
            when (which) {
                0 -> viewPdf(ctx, file)
                1 -> savePdfToDownloads(ctx, file)
                2 -> sharePdf(ctx, file)
            }
        }
        .show()
}

fun viewPdf(ctx: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        ctx.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(ctx, "PDF açıcı bulunamadı.", Toast.LENGTH_SHORT).show()
    }
}

fun savePdfToDownloads(ctx: Context, file: File) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = ctx.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                Toast.makeText(ctx, "PDF İndirilenler klasörüne kaydedildi.", Toast.LENGTH_LONG).show()
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, file.name)
            file.copyTo(destFile, overwrite = true)
            Toast.makeText(ctx, "PDF İndirilenler klasörüne kaydedildi.", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(ctx, "Kaydetme hatası: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun sharePdf(ctx: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "PDF Paylaş"))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
