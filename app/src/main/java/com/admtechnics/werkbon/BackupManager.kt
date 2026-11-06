package com.admtechnics.werkbon

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {
    private const val AUTO_BACKUP_FILE = "auto_backup.json"
    private const val LAST_BACKUP_DATE = "last_backup_date.txt"

    // Her gün otomatik yedek alır
    fun autoBackupIfNeeded(ctx: Context) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            
            val lastBackupFile = File(ctx.filesDir, LAST_BACKUP_DATE)
            var lastBackupDate = ""
            if (lastBackupFile.exists()) {
                lastBackupDate = lastBackupFile.readText().trim()
            }

            // Eğer bugün yedek alınmadıysa al
            if (lastBackupDate != today) {
                createBackup(ctx, isAuto = true)
                lastBackupFile.writeText(today)
            }
        } catch (e: Exception) {
            // Otomatik yedek hatasını kullanıcıya gösterme, sessizce geç
        }
    }

    // Yedek oluşturur
    fun createBackup(ctx: Context, isAuto: Boolean = false): File {
        val outFile = if (isAuto) {
            File(ctx.filesDir, AUTO_BACKUP_FILE)
        } else {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            File(ctx.filesDir, "Werkbon_Backup_$dateStr.json")
        }

        // Bütün verileri tek bir JSON'a topla
        val rootObj = org.json.JSONObject()
        
        val dataFile = File(ctx.filesDir, "werkbon_data.json")
        if (dataFile.exists()) rootObj.put("werkbon_data", org.json.JSONObject(dataFile.readText()))
        
        val weeksFile = File(ctx.filesDir, "werkbon_weeks.json")
        if (weeksFile.exists()) rootObj.put("werkbon_weeks", org.json.JSONObject(weeksFile.readText()))
        
        val expensesFile = File(ctx.filesDir, "werkbon_expenses.json")
        if (expensesFile.exists()) rootObj.put("werkbon_expenses", org.json.JSONObject(expensesFile.readText()))

        outFile.writeText(rootObj.toString())
        return outFile
    }

    // Yedeği geri yükler
    fun restoreData(ctx: Context, backupFile: File) {
        val rootObj = org.json.JSONObject(backupFile.readText())
        
        if (rootObj.has("werkbon_data")) {
            File(ctx.filesDir, "werkbon_data.json").writeText(rootObj.getJSONObject("werkbon_data").toString())
        }
        if (rootObj.has("werkbon_weeks")) {
            File(ctx.filesDir, "werkbon_weeks.json").writeText(rootObj.getJSONObject("werkbon_weeks").toString())
        }
        if (rootObj.has("werkbon_expenses")) {
            File(ctx.filesDir, "werkbon_expenses.json").writeText(rootObj.getJSONObject("werkbon_expenses").toString())
        }
    }
}
