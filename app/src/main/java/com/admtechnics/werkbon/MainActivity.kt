package com.admtechnics.werkbon

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.admtechnics.werkbon.ui.theme.WerkbonTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WerkbonTheme {
                WerkbonApp(applicationContext)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WerkbonApp(ctx: Context) {
    var state by remember { mutableStateOf(Store.load(ctx)) }
    var tab by remember { mutableStateOf(0) }
    val change: (AppState) -> Unit = { s ->
        state = s
        Store.save(ctx, s)
    }

    // Otomatik Yedekleme Kontrolü (Her açılışta çalışır)
    LaunchedEffect(Unit) {
        BackupManager.autoBackupIfNeeded(ctx)
    }

    data class TabItem(val title: String, val icon: ImageVector)
    val tabs = listOf(
        TabItem("Werkbon", Icons.Default.Assignment),
        TabItem("Özet", Icons.Default.BarChart),
        TabItem("Projeler", Icons.Default.Folder),
        TabItem("Proforma", Icons.Default.Description),
        TabItem("Ayarlar", Icons.Default.Settings)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tabs[tab].title, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                tabs.forEachIndexed { i, item ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontSize = 10.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> WerkbonScreen(state)
                1 -> OzetScreen(state) // Bu dosya güncellenecek
                2 -> ProjectsScreen(state, change) // Bu dosya güncellenecek
                3 -> ProformaScreen(state, change) // YENİ
                4 -> SettingsScreen(state, change) // Ayarlar içine yedekleme eklendi
            }
        }
    }
}

@Composable
fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
fun Field(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) },
        singleLine = true, shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    )
}

fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

@Composable
fun SettingsScreen(state: AppState, onChange: (AppState) -> Unit) {
    val ctx = LocalContext.current
    var newWorker by remember { mutableStateOf("") }
    var newCat by remember { mutableStateOf("") }

    // Yedekleme Dosyası Seçici
    val backupFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = ctx.contentResolver.openInputStream(uri)
                val backupFile = File(ctx.filesDir, "restored_data.json")
                val outputStream = FileOutputStream(backupFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                
                // Geri Yükleme işlemi
                BackupManager.restoreData(ctx, backupFile)
                onChange(Store.load(ctx)) // State'i yenile
                Toast.makeText(ctx, "Yedek başarıyla geri yüklendi! Lütfen uygulamayı kapatıp açın.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(ctx, "Geri yükleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = ctx.contentResolver.openInputStream(uri)
                val logoFile = File(ctx.filesDir, "company_logo.jpg")
                val outputStream = FileOutputStream(logoFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                onChange(state.copy(compLogoPath = logoFile.absolutePath))
            } catch (e: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        
        // YEDEKLEME KARTI
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Title("💾 Veri Yedekleme")
                Text("Verileriniz her gün otomatik yedeklenir. Cihaz değiştirirseniz dosyayı dışa aktarıp yeni cihaza yükleyebilirsiniz.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            try {
                                val backupFile = BackupManager.createBackup(ctx)
                                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", backupFile)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                ctx.startActivity(Intent.createChooser(intent, "Yedek Dosyasını Kaydet/Gönder"))
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Yedekleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("📤 Yedekle (Dışa Aktar)") }
                    
                    OutlinedButton(
                        onClick = { backupFilePicker.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("📥 Geri Yükle") }
                }
            }
        }

        // FİRMA BİLGİLERİ KARTI
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Title("Firma Bilgileri")
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { logoPicker.launch("image/*") }, shape = RoundedCornerShape(8.dp)) {
                        Text(if (state.compLogoPath.isEmpty()) "🖼️ Logo Ekle" else "🖼️ Logoyu Değiştir")
                    }
                    if (state.compLogoPath.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            val f = File(state.compLogoPath)
                            if (f.exists()) f.delete()
                            onChange(state.copy(compLogoPath = ""))
                        }) { Text("Logoyu Sil", color = MaterialTheme.colorScheme.error) }
                    }
                }
                Field("Firma Adı", state.compName) { onChange(state.copy(compName = it)) }
                Field("Adres", state.compAddress) { onChange(state.copy(compAddress = it)) }
                Field("E-posta", state.compEmail) { onChange(state.copy(compEmail = it)) }
                Field("GSM", state.compGsm) { onChange(state.copy(compGsm = it)) }
                Field("BTW", state.compBtw) { onChange(state.copy(compBtw = it)) }
            }
        }

        // ÇALIŞANLAR KARTI
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Title("Çalışanlar ve Saatlik Ücret")
                state.workers.forEachIndexed { i, w ->
                    WorkerRow(
                        w = w,
                        canDelete = state.workers.size > 1,
                        onRate = { r -> onChange(state.copy(workers = state.workers.toMutableList().also { l -> l[i] = w.copy(rate = r) })) },
                        onDelete = { onChange(state.copy(workers = state.workers.toMutableList().also { l -> l.removeAt(i) })) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newWorker, onValueChange = { newWorker = it }, label = { Text("Yeni çalışan") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val n = newWorker.trim()
                        if (n.isNotEmpty() && state.workers.none { it.name == n }) {
                            onChange(state.copy(workers = state.workers + Worker(n, 20.0)))
                            newWorker = ""
                        }
                    }, shape = RoundedCornerShape(10.dp)) { Text("Ekle") }
                }
            }
        }

        // GİDER KATEGORİLERİ KARTI
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Title("Gider Kategorileri")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newCat, onValueChange = { newCat = it }, label = { Text("Yeni kategori") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val c = newCat.trim()
                        if (c.isNotEmpty() && c !in state.categories) {
                            onChange(state.copy(categories = state.categories + c))
                            newCat = ""
                        }
                    }, shape = RoundedCornerShape(10.dp)) { Text("Ekle") }
                }
                Spacer(Modifier.height(8.dp))
                state.categories.forEachIndexed { i, c ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("• $c", Modifier.weight(1f))
                        if (state.categories.size > 1) {
                            TextButton(onClick = { onChange(state.copy(categories = state.categories.toMutableList().also { l -> l.removeAt(i) })) }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerRow(w: Worker, canDelete: Boolean, onRate: (Double) -> Unit, onDelete: () -> Unit) {
    var txt by remember(w.name) { mutableStateOf(fmt(w.rate)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(w.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = txt, onValueChange = { v -> txt = v; v.replace(',', '.').toDoubleOrNull()?.let { onRate(it) } },
            modifier = Modifier.width(100.dp), singleLine = true, label = { Text("Ücret", fontSize = 10.sp) },
            shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        if (canDelete) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDelete) { Text("Sil", color = MaterialTheme.colorScheme.error) }
        }
    }
}
