package com.admtechnics.werkbon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.admtechnics.werkbon.ui.theme.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ctx = applicationContext
            var state by remember { mutableStateOf(Store.load(ctx)) }
            val primaryColor = getPrimaryColor(state.themeName)

            WerkbonTheme(primaryColor = primaryColor) {
                WerkbonApp(state) { newState ->
                    state = newState
                    Store.save(ctx, newState)
                }
            }
        }
    }
}

@Composable
fun WerkbonApp(state: AppState, onChange: (AppState) -> Unit) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { BackupManager.autoBackupIfNeeded(ctx) }

    data class TabItem(val title: String, val icon: ImageVector)
    val tabs = listOf(
        TabItem("Werkbon", Icons.Default.Assignment),
        TabItem("Özet", Icons.Default.BarChart),
        TabItem("Aylık", Icons.Default.CalendarMonth),
        TabItem("Projeler", Icons.Default.Folder),
        TabItem("Proforma", Icons.Default.Description),
        TabItem("Ayarlar", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, item ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(20.dp)) },
                        label = { Text(item.title, fontSize = 9.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) },
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
                0 -> WerkbonScreen(state, tabs[tab].title)
                1 -> OzetScreen(state, tabs[tab].title) 
                2 -> AylikRaporScreen(state, tabs[tab].title)
                3 -> ProjectsScreen(state, onChange)
                4 -> ProformaScreen(state, onChange, tabs[tab].title)
                5 -> SettingsScreen(state, onChange, tabs[tab].title)
            }
        }
    }
}

// TÜM SAYFALARDA KULLANILAN STANDART BAŞLIK KARTI
@Composable
fun TopHeaderCard(title: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 12.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
}

// TÜM YAZI KUTULARI İÇİN STANDART (Firma Adı, Adres, Yeni Çalışan vs.)
@Composable
fun Field(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value, 
        onValueChange = onValue, 
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true, 
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.LightGray,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun PremiumCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(Modifier.padding(12.dp), content = content)
    }
}

@Composable
fun SettingsScreen(state: AppState, onChange: (AppState) -> Unit, title: String) {
    val ctx = LocalContext.current
    var newWorker by remember { mutableStateOf("") }
    var newCat by remember { mutableStateOf("") }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerAddress by remember { mutableStateOf("") }
    var newCustomerBtw by remember { mutableStateOf("") }

    // Düzenleme Modu İçin State'ler
    var editWorkerIndex by remember { mutableStateOf(-1) }
    var editCatIndex by remember { mutableStateOf(-1) }
    var editCustomerIndex by remember { mutableStateOf(-1) }

    val backupFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = ctx.contentResolver.openInputStream(uri)
                val backupFile = File(ctx.filesDir, "restored_data.json")
                val outputStream = FileOutputStream(backupFile)
                inputStream?.copyTo(outputStream); inputStream?.close(); outputStream.close()
                BackupManager.restoreData(ctx, backupFile)
                onChange(Store.load(ctx))
                Toast.makeText(ctx, "Yedek geri yüklendi!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) { Toast.makeText(ctx, "Hata: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = ctx.contentResolver.openInputStream(uri)
                val logoFile = File(ctx.filesDir, "company_logo.jpg")
                val outputStream = FileOutputStream(logoFile)
                inputStream?.copyTo(outputStream); inputStream?.close(); outputStream.close()
                onChange(state.copy(compLogoPath = logoFile.absolutePath))
            } catch (e: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        TopHeaderCard(title)

        PremiumCard {
            Title("🎨 Uygulama Tema Rengi")
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                val colors = listOf("blue" to ThemeBlue, "green" to ThemeGreen, "red" to ThemeRed, "orange" to ThemeOrange, "purple" to ThemePurple, "dark" to ThemeDarkGray)
                colors.forEach { (name, color) ->
                    Box(modifier = Modifier.size(32.dp).background(color, CircleShape).clickable { onChange(state.copy(themeName = name)) }.then(if (state.themeName == name) Modifier.border(2.dp, Color.Black, CircleShape) else Modifier))
                }
            }
        }

        PremiumCard {
            Title("💾 Veri Yedekleme")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { 
                    try {
                        val backupFile = BackupManager.createBackup(ctx)
                        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", backupFile)
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "application/octet-stream"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        ctx.startActivity(Intent.createChooser(intent, "Yedek Dosyasını Kaydet/Gönder"))
                    } catch (e: Exception) { Toast.makeText(ctx, "Yedekleme hatası", Toast.LENGTH_LONG).show() }
                }, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp)) { Text("📤 Yedekle", fontSize = 12.sp) }
                
                OutlinedButton(onClick = { backupFilePicker.launch("*/*") }, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp)) { Text("📥 Geri Yükle", fontSize = 12.sp) }
            }
        }

        PremiumCard {
            Title("Firma Bilgileriniz")
            OutlinedButton(onClick = { logoPicker.launch("image/*") }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(40.dp)) { Text(if (state.compLogoPath.isEmpty()) "🖼️ Logo Ekle" else "🖼️ Logoyu Değiştir", fontSize = 12.sp) }
            Spacer(Modifier.height(8.dp))
            Field("Firma Adı", state.compName) { onChange(state.copy(compName = it)) }
            Field("Adres", state.compAddress) { onChange(state.copy(compAddress = it)) }
            Field("E-posta", state.compEmail) { onChange(state.copy(compEmail = it)) }
            Field("GSM", state.compGsm) { onChange(state.copy(compGsm = it)) }
            Field("BTW", state.compBtw) { onChange(state.copy(compBtw = it)) }
        }

        PremiumCard {
            Title("Firmalar / Müşteriler")
            state.customers.forEachIndexed { i, c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(c.name, fontWeight = FontWeight.Medium, fontSize = 13.sp); Text(c.btw, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    TextButton(onClick = { editCustomerIndex = i }, modifier = Modifier.height(30.dp)) { Text("✏️ Düzenle", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp) }
                    TextButton(onClick = { onChange(state.copy(customers = state.customers.toMutableList().also { it.removeAt(i) })) }, modifier = Modifier.height(30.dp)) { Text("Sil", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                }
                Divider(color = Color.LightGray.copy(alpha=0.5f))
            }
            Spacer(Modifier.height(8.dp))
            Field("Yeni Firma Adı", newCustomerName) { newCustomerName = it }
            Field("Adres", newCustomerAddress) { newCustomerAddress = it }
            Field("BTW Numarası", newCustomerBtw) { newCustomerBtw = it }
            Button(onClick = { 
                if (newCustomerName.isNotBlank()) { 
                    if (editCustomerIndex >= 0) {
                        val updated = state.customers.toMutableList()
                        updated[editCustomerIndex] = Customer(newCustomerName.trim(), newCustomerAddress.trim(), newCustomerBtw.trim())
                        onChange(state.copy(customers = updated))
                        editCustomerIndex = -1
                    } else {
                        onChange(state.copy(customers = state.customers + Customer(newCustomerName.trim(), newCustomerAddress.trim(), newCustomerBtw.trim())))
                    }
                    newCustomerName = ""; newCustomerAddress = ""; newCustomerBtw = "" 
                } 
            }, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(8.dp)) { Text(if (editCustomerIndex >= 0) "Firmayı Güncelle" else "Firmayı Kaydet", fontSize = 13.sp) }
        }

        PremiumCard {
            Title("Çalışanlar ve Saatlik Ücretler")
            state.workers.forEachIndexed { i, w ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(w.name, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    // Ücret Kutusu (Standart)
                    OutlinedTextField(
                        value = fmt(w.rate), 
                        onValueChange = { v -> v.replace(',', '.').toDoubleOrNull()?.let { nr -> onChange(state.copy(workers = state.workers.toMutableList().also { l -> l[i] = w.copy(rate = nr) })) } }, 
                        modifier = Modifier.width(80.dp).height(52.dp), 
                        singleLine = true, shape = RoundedCornerShape(8.dp), 
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextButton(onClick = { editWorkerIndex = i }, modifier = Modifier.height(30.dp)) { Text("✏️", fontSize = 16.sp) }
                    if (state.workers.size > 1) { TextButton(onClick = { onChange(state.copy(workers = state.workers.toMutableList().also { l -> l.removeAt(i) })) }, modifier = Modifier.height(30.dp)) { Text("Sil", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) } }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newWorker, onValueChange = { newWorker = it }, label = { Text("Yeni çalışan", fontSize=11.sp) }, singleLine = true, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(8.dp), textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp))
                Spacer(Modifier.width(8.dp))
                Button(onClick = { 
                    if (newWorker.isNotBlank()) { 
                        if (editWorkerIndex >= 0) {
                            val updated = state.workers.toMutableList()
                            updated[editWorkerIndex] = Worker(newWorker.trim(), updated[editWorkerIndex].rate)
                            onChange(state.copy(workers = updated))
                            editWorkerIndex = -1
                        } else {
                            onChange(state.copy(workers = state.workers + Worker(newWorker.trim(), 20.0)))
                        }
                        newWorker = "" 
                    } 
                }, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(40.dp)) { Text(if (editWorkerIndex >= 0) "Güncelle" else "Ekle", fontSize = 12.sp) }
            }
        }

        PremiumCard {
            Title("Gider / Malzeme Kategorileri")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newCat, onValueChange = { newCat = it }, label = { Text("Yeni kategori", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(8.dp), textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp))
                Spacer(Modifier.width(8.dp))
                Button(onClick = { 
                    if (newCat.isNotBlank()) { 
                        if (editCatIndex >= 0) {
                            val updated = state.categories.toMutableList()
                            updated[editCatIndex] = newCat.trim()
                            onChange(state.copy(categories = updated))
                            editCatIndex = -1
                        } else {
                            if (!state.categories.contains(newCat.trim())) onChange(state.copy(categories = state.categories + newCat.trim()))
                        }
                        newCat = "" 
                    } 
                }, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(40.dp)) { Text(if (editCatIndex >= 0) "Güncelle" else "Ekle", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(8.dp))
            state.categories.forEachIndexed { i, c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("• $c", Modifier.weight(1f), fontSize = 13.sp)
                    TextButton(onClick = { newCat = c; editCatIndex = i }, modifier = Modifier.height(30.dp)) { Text("✏️", fontSize = 16.sp) }
                    if (state.categories.size > 1) { TextButton(onClick = { onChange(state.copy(categories = state.categories.toMutableList().also { l -> l.removeAt(i) })) }, modifier = Modifier.height(30.dp)) { Text("Sil", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) } }
                }
            }
        }
    }
}
