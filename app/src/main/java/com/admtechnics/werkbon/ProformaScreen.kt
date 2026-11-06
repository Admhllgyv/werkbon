package com.admtechnics.werkbon

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProformaScreen(state: AppState, onChange: (AppState) -> Unit) {
    val ctx = LocalContext.current
    var selectedProject by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<ProformaItem>()) }
    
    var newDesc by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("1") }
    var newPrice by remember { mutableStateOf("") }

    val projects = state.projects.map { "${it.code} - ${it.name}" }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Title("Yeni Proforma Oluştur")
                
                Picker("Proje Seçin", if (selectedProject.isEmpty()) "Seçiniz" else selectedProject, listOf("Seçiniz") + projects, { 
                    if (it != "Seçiniz") selectedProject = it else selectedProject = ""
                }, Modifier.fillMaxWidth())
                
                Spacer(Modifier.height(12.dp))
                
                Text("Kalemler", fontWeight = FontWeight.Bold, fontSize
