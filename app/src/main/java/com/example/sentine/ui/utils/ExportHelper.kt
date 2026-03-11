package com.example.sentine.ui.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.sentine.data.db.AppRiskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {
    suspend fun exportReportToDownloads(context: Context, apps: List<AppRiskEntity>): Uri? = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "Sentinel_Report_${dateFormat.format(Date())}.html"

        val htmlContent = buildHtmlReport(apps)

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/html")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SentinelReports")
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(htmlContent)
                }
            }
        }
        return@withContext uri
    }

    private fun buildHtmlReport(apps: List<AppRiskEntity>): String {
        val highRisk = apps.filter { it.riskLevel == "HIGH" }
        val mediumRisk = apps.filter { it.riskLevel == "MEDIUM" }
        val others = apps.filter { it.riskLevel != "HIGH" && it.riskLevel != "MEDIUM" }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>SentinelAI Risk Report</title>
                <style>
                    body { font-family: sans-serif; line-height: 1.6; color: #333; max-width: 800px; margin: 0 auto; padding: 20px; }
                    h1 { color: #1a73e8; }
                    h2 { margin-top: 30px; border-bottom: 2px solid #eee; padding-bottom: 5px; }
                    .high { color: #d32f2f; font-weight: bold; }
                    .medium { color: #f57c00; font-weight: bold; }
                    .safe { color: #388e3c; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    th { background-color: #f2f2f2; }
                </style>
            </head>
            <body>
                <h1>SentinelAI Risk Report</h1>
                <p>Generated on ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())}</p>
                <p>Total Apps Analyzed: <strong>${apps.size}</strong></p>
                
                <h2 class="high">High Risk Apps (${highRisk.size})</h2>
                ${buildTable(highRisk)}
                
                <h2 class="medium">Medium Risk Apps (${mediumRisk.size})</h2>
                ${buildTable(mediumRisk)}
                
                <h2 class="safe">Safe/Low Risk Apps (${others.size})</h2>
                ${buildTable(others)}
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildTable(apps: List<AppRiskEntity>): String {
        if (apps.isEmpty()) return "<p>None</p>"
        val rows = apps.joinToString("\n") { app ->
            "<tr><td>${app.appName}</td><td>${app.packageName}</td><td>${app.riskScore}/100</td></tr>"
        }
        return """
            <table>
                <tr><th>App Name</th><th>Package</th><th>Score</th></tr>
                $rows
            </table>
        """.trimIndent()
    }
}
