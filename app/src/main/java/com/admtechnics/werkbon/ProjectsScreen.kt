package com.admtechnics.werkbon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProjectsScreen(state: AppState, onChange: (AppState) -> Unit) {
    val ctx = LocalContext.current
    var newName by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    
    // Tüm haftalık verileri ve fişleri çekelim
    val weeksData = WeekStore.load(ctx)
    val expensesData = ExpenseStore.load(ctx)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // YENİ PROJE EKLEME KARTI
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Title("Yeni Proje Ekle")
                Field("Proje / Müşteri adı", newName) { newName = it }
                Field("Proje kodu (örn. E260003)", newCode) { newCode = it }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val code = newCode.trim().ifEmpty { "E260000" }
                            onChange(state.copy(projects = state.projects + Project(code, newName.trim())))
                            newName = ""
                            newCode = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Projeyi Kaydet") }
            }
        }

        Title("Mevcut Projeler ve Geçmiş")
        
        // PROJELER LİSTESİ (AÇILIR KARTLAR)
        state.projects.forEachIndexed { i, p ->
            var isExpanded by remember { mutableStateOf(false) }
            val rotationState by animateFloatAsState(if (isExpanded) 180f else 0f)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { isExpanded = !isExpanded }
            ) {
                Column {
                    // Kart Başlığı (Tıklanabilir)
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Kod: ${p.code}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Detayları Gör",
                            modifier = Modifier.rotate(rotationState)
                        )
                    }

                    // Açılır İçerik (Geçmiş Detayları)
                    AnimatedVisibility(visible = isExpanded) {
                        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            Spacer(Modifier.height(8.dp))
                            
                            // Proje bilgilerini düzenleme alanları
                            Text("Proje Bilgilerini Düzenle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Field("Kod", p.code) { v ->
                                onChange(state.copy(projects = state.projects.toMutableList().also { l -> l[i] = p.copy(code = v) }))
                            }
                            Field("Ad", p.name) { v ->
                                onChange(state.copy(projects = state.projects.toMutableList().also { l -> l[i] = p.copy(name = v) }))
                            }
                            
                            TextButton(
                                onClick = {
                                    onChange(state.copy(projects = state.projects.toMutableList().also { l -> l.removeAt(i) }))
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("Projeyi Sil", color = MaterialTheme.colorScheme.error) }

                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            Spacer(Modifier.height(8.dp))

                            Text("Çalışma Geçmişi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            val projectFullName = "${p.code} - ${p.name}"
                            
                            // Tüm haftalardan bu projeye ait kayıtları bul
                            val projEntries = weeksData.values.flatten().filter { it.project == projectFullName }
                            
                            if (projEntries.isEmpty()) {
                                Text("Bu projede henüz çalışma kaydı yok.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                val totalHours = r2(projEntries.sumOf { it.hours() })
                                Text("Toplam Çalışma: $totalHours Saat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                
                                projEntries.sortedBy { it.day }.forEach { e ->
                                    Text("• ${dayTr[e.day]}: ${e.worker} (${hm(e.start)} - ${hm(e.end)}) = ${fmt(e.hours())} saat", fontSize = 12.sp)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text("Fiş Geçmişi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            // Tüm haftalardan bu projeye ait fişleri bul
                            val projExpenses = expensesData.values.flatten().filter { it.project == projectFullName }
                            
                            if (projExpenses.isEmpty()) {
                                Text("Bu projede henüz fiş kaydı yok.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                val totalCost = r2(projExpenses.sumOf { it.amount })
                                Text("Toplam Gider: €$totalCost", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                
                                projExpenses.sortedBy { it.day }.forEach { ex ->
                                    Text("• ${dayTr[ex.day]}: ${ex.category} - €${ex.amount}", fontSize = 12.sp)
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
