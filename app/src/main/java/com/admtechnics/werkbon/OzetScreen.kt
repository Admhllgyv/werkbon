package com.admtechnics.werkbon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border // <-- EKLENDİ
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.time.format.DateTimeFormatter

@Composable
fun OzetScreen(state: AppState, title: String) {
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        TopHeaderCard(title)
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { shift(-1L) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(60.dp)) { Text("◀", fontSize = 14.sp) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$year - Hafta $week", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Text("${mon.format(dateFmt)} – ${mon.plusDays(4).format(dateFmt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { shift(1L) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(60.dp)) { Text("▶", fontSize = 14.sp) }
            }
        }

        if (entries.isEmpty() && expenses.isEmpty()) {
            Text("Bu hafta için kayıt bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        if (entries.isNotEmpty()) {
            Text("📋 Tüm Çalışma Kayıtları", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 8.dp))
            
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))) {
                Row(Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(vertical = 8.dp)) {
                    TableHeader("Tarih & Gün", 120.dp)
                    TableHeader("Proje", 140.dp)
                    TableHeader("Çalışan", 80.dp)
                    TableHeader("Saat", 60.dp)
                }
                entries.sortedBy { it.day }.forEachIndexed { index, e ->
                    val bgColor = if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    Row(Modifier.background(bgColor).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TableCell("${mon.plusDays(e.day.toLong()).format(dateFmt)} / ${dayTr[e.day]}", 120.dp)
                        TableCell(e.project.substringAfter("- ").take(15), 140.dp) 
                        TableCell(e.worker, 80.dp)
                        TableCell("${fmt(e.hours())}", 60.dp, FontWeight.Bold, MaterialTheme.colorScheme.primary)
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (expenses.isNotEmpty()) {
            Text("💶 Fiş / Gider Detayları", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 8.dp))
            
            Column(Modifier.fillMaxWidth()) {
                expenses.sortedBy { it.day }.forEach { ex ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (ex.image.isNotEmpty()) {
                                val file = File(receiptsDir(ctx), ex.image)
                                if (file.exists()) {
                                    val bmp = decodeSampled(file, 8)
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = "Fiş",
                                            modifier = Modifier.size(50.dp).padding(end = 8.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            
                            Column(Modifier.weight(1f)) {
                                Text(ex.category, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${dayTr[ex.day]} - ${ex.project.substringAfter("- ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("€${ex.amount}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    Text("Toplam Gider: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("€${r2(expenses.sumOf { it.amount })}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (entries.isNotEmpty()) {
            Text("👷 Çalışan Bazlı Toplam Saatler", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 8.dp))
            val workerHours = entries.groupBy { it.worker }.mapValues { r2(it.value.sumOf { e -> e.hours() }) }
            
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))) {
                Row(Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(vertical = 8.dp)) {
                    TableHeader("Çalışan", 150.dp)
                    TableHeader("Saat", 100.dp)
                }
                workerHours.forEach { (worker, hours) ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        TableCell(worker, 150.dp)
                        TableCell("$hours", 100.dp)
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (entries.isNotEmpty()) {
            Text("📁 Proje Bazlı Toplam Saatler", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 8.dp))
            val projHours = entries.groupBy { it.project }.mapValues { r2(it.value.sumOf { e -> e.hours() }) }
            
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))) {
                Row(Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(vertical = 8.dp)) {
                    TableHeader("Proje", 250.dp)
                    TableHeader("Saat", 100.dp)
                }
                projHours.forEach { (proj, hours) ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        TableCell(proj, 250.dp)
                        TableCell("$hours", 100.dp)
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun TableHeader(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(text = text, modifier = Modifier.width(width).padding(horizontal = 8.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
}

@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, weight: FontWeight = FontWeight.Normal, color: Color = Color.Black) {
    Text(text = text, modifier = Modifier.width(width).padding(horizontal = 8.dp), fontSize = 12.sp, fontWeight = weight, color = color, maxLines = 1)
}
