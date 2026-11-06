package com.admtechnics.werkbon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProjectsScreen(state: AppState, onChange: (AppState) -> Unit) {
    val ctx = LocalContext.current
    var newName by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf("") } // YENİ
    
    val customerNames = state.customers.map { it.name }

    val weeksData = WeekStore.load(ctx)
    val expensesData = ExpenseStore.load(ctx)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Title("Yeni Proje Ekle")
                Field("Proje / İş adı (örn. Stekene)", newName) { newName = it }
                Field("Proje kodu (örn. E260320)", newCode) { newCode = it }
                
                // Müşteri Seçimi
                if (customerNames.isNotEmpty()) {
                    Picker("Firma / Müşteri Seçin", if (selectedCustomer.isEmpty()) "Seçiniz" else selectedCustomer, listOf("Seçiniz") + customerNames, { 
                        if (it != "Seçiniz") selectedCustomer = it else selectedCustomer = ""
                    }, Modifier.fillMaxWidth())
                } else {
                    Text("Önce Ayarlar'dan bir firma ekleyin.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newName.isNotBlank() && selectedCustomer.isNotEmpty()) {
                            val code = newCode.trim().ifEmpty { "E260000" }
                            onChange(state.copy(projects = state.projects + Project(code, newName.trim(), selectedCustomer)))
                            newName = ""; newCode = ""; selectedCustomer = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Projeyi Kaydet") }
            }
        }

        Title("Mevcut Projeler ve Geçmiş")
        
        state.projects.forEachIndexed { i, p ->
            var isExpanded by remember { mutableStateOf(false) }
            val rotationState by animateFloatAsState(if (isExpanded) 180f else 0f)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { isExpanded = !isExpanded }) {
                Column {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Kod: ${p.code} | Firma: ${p.customerName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, "Detaylar", modifier = Modifier.rotate(rotationState))
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            Spacer(Modifier.height(8.dp))
                            
                            Text("Proje Bilgilerini Düzenle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Field("Kod", p.code) { v -> onChange(state.copy(projects = state.projects.toMutableList().also { l -> l[i] = p.copy(code = v) })) }
                            Field("Ad", p.name) { v -> onChange(state.copy(projects = state.projects.toMutableList().also { l -> l[i] = p.copy(name = v) })) }
                            Picker("Firma", p.customerName, customerNames, { v -> onChange(state.copy(projects = state.projects.toMutableList().also { l -> l[i] = p.copy(customerName = v) })) }, Modifier.fillMaxWidth())
                            
                            TextButton(onClick = { onChange(state.copy(projects = state.projects.toMutableList().also { l -> l.removeAt(i) })) }, modifier = Modifier.align(Alignment.End)) { Text("Projeyi Sil", color = MaterialTheme.colorScheme.error) }

                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            Spacer(Modifier.height(8.dp))

                            val projectFullName = "${p.code} - ${p.name}"
                            
                            // Çalışma Geçmişi
                            val projEntries = weeksData.values.flatten().filter { it.project == projectFullName }
                            Text("Çalışma Geçmişi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (projEntries.isEmpty()) {
                                Text("Kayıt yok.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("Toplam: ${r2(projEntries.sumOf { it.hours() })} Saat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                projEntries.sortedBy { it.day }.forEach { e ->
                                    Text("• ${dayTr[e.day]}: ${e.worker} (${fmt(e.hours())} sa)", fontSize = 12.sp)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            
                            // Fiş Geçmişi
                            val projExpenses = expensesData.values.flatten().filter { it.project == projectFullName }
                            Text("Fiş Geçmişi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (projExpenses.isEmpty()) {
                                Text("Kayıt yok.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("Toplam: €${r2(projExpenses.sumOf { it.amount })}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
