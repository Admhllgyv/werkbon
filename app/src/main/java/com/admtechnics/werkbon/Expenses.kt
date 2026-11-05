package com.admtechnics.werkbon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Expense(
    val id: String,
    val day: Int,
    val project: String,
    val category: String,
    val amount: Double,
    val image: String
)

object ExpenseStore {
    private const val FILE_NAME = "werkbon_expenses.json"

    fun load(ctx: Context): Map<String, List<Expense>> {
        val f = File(ctx.filesDir, FILE_NAME)
        if (!f.exists()) return emptyMap()
        return try {
            val o = JSONObject(f.readText())
            val out = LinkedHashMap<String, List<Expense>>()
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = o.getJSONArray(k)
                val list = ArrayList<Expense>()
                for (i in 0 until arr.length()) {
                    val x = arr.getJSONObject(i)
                    list.add(
                        Expense(
                            x.getString("id"),
                            x.getInt("day"),
                            x.getString("project"),
                            x.getString("category"),
                            x.optDouble("amount", 0.0),
                            x.optString("image", "")
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

    fun save(ctx: Context, m: Map<String, List<Expense>>) {
        val o = JSONObject()
        for ((k, list) in m) {
            val arr = JSONArray()
            for (e in list) {
                arr.put(
                    JSONObject()
                        .put("id", e.id)
                        .put("day", e.day)
                        .put("project", e.project)
                        .put("category", e.category)
                        .put("amount", e.amount)
                        .put("image", e.image)
                )
            }
            o.put(k, arr)
        }
        File(ctx.filesDir, FILE_NAME).writeText(o.toString())
    }
}

fun receiptsDir(ctx: Context): File {
    val d = File(ctx.filesDir, "receipts")
    d.mkdirs()
    return d
}

fun downscaleReceipt(file: File) {
    try {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) sample *= 2
        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        var bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val deg = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (deg != 0f) {
            val m = Matrix()
            m.postRotate(deg)
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        val scale = 1600f / maxOf(bmp.width, bmp.height)
        if (scale < 1f) {
            bmp = Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * scale).toInt(),
                (bmp.height * scale).toInt(),
                true
            )
        }
        file.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 80, out) }
    } catch (e: Exception) {
        // büyük/bozuk dosyada orijinali olduğu gibi bırak
    }
}

fun decodeSampled(file: File, sample: Int): ImageBitmap? {
    val o = BitmapFactory.Options()
    o.inSampleSize = sample
    return BitmapFactory.decodeFile(file.absolutePath, o)?.asImageBitmap()
}

@Composable
fun Thumb(file: File) {
    var big by remember { mutableStateOf(false) }
    val small = remember(file.path) { decodeSampled(file, 4) }
    if (small != null) {
        Image(
            bitmap = small,
            contentDescription = "Fiş",
            modifier = Modifier.height(72.dp).clickable { big = true }
        )
    }
    if (big) {
        val large = remember(file.path) { decodeSampled(file, 2) }
        Dialog(onDismissRequest = { big = false }) {
            if (large != null) {
                Image(
                    bitmap = large,
                    contentDescription = "Fiş",
                    modifier = Modifier.fillMaxWidth().clickable { big = false },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun ExpenseCard(
    e: Expense,
    projects: List<String>,
    cats: List<String>,
    onChange: (Expense) -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    var txt by remember(e.id) { mutableStateOf(if (e.amount == 0.0) "" else fmt(e.amount)) }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(8.dp)) {
            // Gün seçici silindi, çünkü gün dışarıdan geliyor
            Picker("Proje", e.project, projects, { v -> onChange(e.copy(project = v)) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Picker("Gider", e.category, cats, { v -> onChange(e.copy(category = v)) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = txt,
                onValueChange = { v ->
                    txt = v
                    onChange(e.copy(amount = v.replace(',', '.').toDoubleOrNull() ?: 0.0))
                },
                label = { Text("Tutar (€)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCamera) { Text("📷 Çek") }
                OutlinedButton(onClick = onGallery) { Text("🖼 Galeri") }
                if (e.image.isNotEmpty()) {
                    Thumb(File(receiptsDir(ctx), e.image))
                }
            }
            TextButton(onClick = onDelete) { Text("Fişi sil") }
        }
    }
}

@Composable
fun ExpensesSection(state: AppState, key: String, day: Int) {
    val ctx = LocalContext.current
    
    val allExpenses = ExpenseStore.load(ctx)[key] ?: emptyList()
    var items by remember(key, day, allExpenses) { 
        mutableStateOf(allExpenses.filter { it.day == day }) 
    }
    
    var pendingFile by rememberSaveable { mutableStateOf("") }
    var pendingIdx by rememberSaveable { mutableStateOf(-1) }
    val projects = state.projects.sortedByDescending { it.code }.map { "${it.code} - ${it.name}" }
    val cats = state.categories

    fun update(list: List<Expense>) {
        val currentAll = ExpenseStore.load(ctx)[key] ?: emptyList()
        val otherDays = currentAll.filter { it.day != day }
        val newListForAll = (otherDays + list).sortedBy { it.day }
        
        val all = LinkedHashMap(ExpenseStore.load(ctx))
        all[key] = newListForAll
        ExpenseStore.save(ctx, all)
        
        items = list
    }

    fun attachImage(name: String) {
        if (pendingIdx in items.indices) {
            val l = items.toMutableList()
            l[pendingIdx] = l[pendingIdx].copy(image = name)
            update(l)
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val name = pendingFile
        if (ok && name.isNotEmpty()) {
            downscaleReceipt(File(receiptsDir(ctx), name))
            attachImage(name)
        }
        pendingFile = ""
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = "r_${System.currentTimeMillis()}.jpg"
            val f = File(receiptsDir(ctx), name)
            try {
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    f.outputStream().use { out -> input.copyTo(out) }
                }
                downscaleReceipt(f)
                attachImage(name)
            } catch (ex: Exception) {
                // seçilen dosya okunamadı
            }
        }
    }

    fun takePhoto(idx: Int) {
        val name = "r_${System.currentTimeMillis()}.jpg"
        val f = File(receiptsDir(ctx), name)
        f.createNewFile()
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        pendingFile = name
        pendingIdx = idx
        camera.launch(uri)
    }

    val total = r2(items.map { it.amount }.sum())
    val totalTxt = "%.2f".format(total)

    Column(Modifier.fillMaxWidth()) {
        if (items.isNotEmpty()) {
            Text("Bu günün fiş toplamı: €$totalTxt", fontWeight = FontWeight.Bold)
        }
        items.forEachIndexed { idx, e ->
            ExpenseCard(
                e = e,
                projects = projects,
                cats = cats,
                onChange = { ne ->
                    update(items.toMutableList().also { l -> l[idx] = ne })
                },
                onCamera = { takePhoto(idx) },
                onGallery = {
                    pendingIdx = idx
                    gallery.launch("image/*")
                },
                onDelete = {
                    if (e.image.isNotEmpty()) File(receiptsDir(ctx), e.image).delete()
                    update(items.toMutableList().also { l -> l.removeAt(idx) })
                }
            )
        }
        if (projects.isNotEmpty() && cats.isNotEmpty()) {
            Button(onClick = {
                update(
                    items + Expense(
                        id = System.currentTimeMillis().toString(),
                        day = day,
                        project = projects[0],
                        category = cats[0],
                        amount = 0.0,
                        image = ""
                    )
                )
            }) { Text("+ Fiş ekle") }
        }
    }
}
