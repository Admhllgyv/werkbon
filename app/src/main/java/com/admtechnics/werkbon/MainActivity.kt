package com.admtechnics.werkbon

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WerkbonApp(applicationContext)
            }
        }
    }
}

@Composable
fun WerkbonApp(ctx: Context) {
    var state by remember { mutableStateOf(Store.load(ctx)) }
    var tab by remember { mutableStateOf(0) }
    val change: (AppState) -> Unit = { s ->
        state = s
        Store.save(ctx, s)
    }
    val tabs = listOf("Werkbon", "Özet", "Projeler", "Ayarlar")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {},
                        label = { Text(t) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                2 -> ProjectsScreen(state, change)
                3 -> SettingsScreen(state, change)
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bu ekran sonraki adımda eklenecek")
                }
            }
        }
    }
}

@Composable
fun Title(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun Field(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

@Composable
fun WorkerRow(w: Worker, canDelete: Boolean, onRate: (Double) -> Unit, onDelete: () -> Unit) {
    var txt by remember(w.name) { mutableStateOf(fmt(w.rate)) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(w.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = txt,
            onValueChange = { v ->
                txt = v
                v.replace(',', '.').toDoubleOrNull()?.let { onRate(it) }
            },
            modifier = Modifier.width(100.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        if (canDelete) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDelete) { Text("Sil") }
        }
    }
}

@Composable
fun SettingsScreen(state: AppState, onChange: (AppState) -> Unit) {
    var newWorker by remember { mutableStateOf("") }
    var newCat by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        Title("Firma Bilgileri")
        Field("Firma Adı", state.compName) { onChange(state.copy(compName = it)) }
        Field("Adres", state.compAddress) { onChange(state.copy(compAddress = it)) }
        Field("E-posta", state.compEmail) { onChange(state.copy(compEmail = it)) }
        Field("GSM", state.compGsm) { onChange(state.copy(compGsm = it)) }
        Field("BTW", state.compBtw) { onChange(state.copy(compBtw = it)) }

        Spacer(Modifier.height(20.dp))
        Title("Çalışanlar ve Saatlik Ücret (€)")
        state.workers.forEachIndexed { i, w ->
            WorkerRow(
                w = w,
                canDelete = state.workers.size > 1,
                onRate = { r ->
                    onChange(state.copy(workers = state.workers.toMutableList().also { l -> l[i] = w.copy(rate = r) }))
                },
                onDelete = {
                    onChange(state.copy(workers = state.workers.toMutableList().also { l -> l.removeAt(i) }))
                }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newWorker,
                onValueChange = { newWorker = it },
                label = { Text("Yeni çalışan") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val n = newWorker.trim()
                if (n.isNotEmpty() && state.workers.none { it.name == n }) {
                    onChange(state.copy(workers = state.workers + Worker(n, 20.0)))
                    newWorker = ""
                }
            }) { Text("Ekle") }
        }

        Spacer(Modifier.height(20.dp))
        Title("Gider / Malzeme Kategorileri")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCat,
                onValueChange = { newCat = it },
                label = { Text("Yeni kategori") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val c = newCat.trim()
                if (c.isNotEmpty() && c !in state.categories) {
                    onChange(state.copy(categories = state.categories + c))
                    newCat = ""
                }
            }) { Text("Ekle") }
        }
        state.categories.forEachIndexed { i, c ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("• $c", Modifier.weight(1f))
                if (state.categories.size > 1) {
                    TextButton(onClick = {
                        onChange(state.copy(categories = state.categories.toMutableList().also { l -> l.removeAt(i) }))
                    }) { Text("Sil") }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ProjectsScreen(state: AppState, onChange: (AppState) -> Unit) {
    var newName by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        Title("Proje Ekle")
        Field("Proje / Müşteri adı", newName) { newName = it }
        Field("Proje kodu (örn. E260003)", newCode) { newCode = it }
        Button(onClick = {
            if (newName.isNotBlank()) {
                val code = newCode.trim().ifEmpty { "E260000" }
                onChange(state.copy(projects = state.projects + Project(code, newName.trim())))
                newName = ""
                newCode = ""
            }
        }) { Text("Projeyi kaydet") }

        Spacer(Modifier.height(20.dp))
        Title("Projeler")
        state.projects.forEachIndexed { i, p ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Field("Kod", p.code) { v ->
                        onChange(state.copy(projects = state.projects.toMutableList().also { l -> l[i] = p.copy(code = v) }))
                    }
                    Field("Ad", p.name) { v ->
                        onChange(state.copy(projects = state.projects.toMutableList().also { l -> l[i] = p.copy(name = v) }))
                    }
                    TextButton(onClick = {
                        onChange(state.copy(projects = state.projects.toMutableList().also { l -> l.removeAt(i) }))
                    }) { Text("Projeyi sil") }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
