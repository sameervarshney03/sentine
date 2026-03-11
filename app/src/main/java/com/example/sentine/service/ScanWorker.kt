package com.example.sentine.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.app.NotificationManager
import android.util.Log
import androidx.work.workDataOf
import com.example.sentine.R
import com.example.sentine.SentinelApp
import com.example.sentine.data.db.AppRiskEntity

class ScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SentinelApp
        val pm  = applicationContext.packageManager
        var scannedCount  = 0
        var highRiskCount = 0

        return try {
            // Get REAL installed user apps
            val userApps = pm
                .getInstalledApplications(
                    PackageManager.GET_META_DATA
                )
                .filter { info ->
                    (info.flags and 
                     ApplicationInfo.FLAG_SYSTEM) == 0 &&
                    info.packageName != 
                        applicationContext.packageName
                }

            Log.d("ScanWorker", 
                "Starting real scan of ${userApps.size} apps")

            userApps.forEach { appInfo ->
                try {
                    // REAL risk calculation
                    val result = app.riskEngine.calculateRisk(
                        appInfo.packageName,
                        10,
                        false,
                        0L
                    )

                    // Get previous score for smoothing
                    val existing = app.database
                        .appRiskDao()
                        .getAppRisk(appInfo.packageName) // Actually called getAppRisk from AppRiskDao

                    // Smooth score: 70% new, 30% old
                    val smoothedScore = if (existing != null) {
                        (result.score * 0.7f + 
                         existing.riskScore * 0.3f).toInt()
                    } else {
                        result.score
                    }

                    val smoothedLevel = when {
                        smoothedScore >= 76 -> "HIGH"
                        smoothedScore >= 51 -> "MEDIUM"
                        smoothedScore >= 26 -> "LOW"
                        else               -> "SAFE"
                    }

                    if (smoothedLevel == "HIGH") highRiskCount++

                    // Save REAL result to database
                    app.database.appRiskDao().insertOrUpdate(
                        AppRiskEntity(
                            packageName  = appInfo.packageName,
                            appName      = pm.getApplicationLabel(
                                              appInfo
                                          ).toString(),
                            riskScore    = smoothedScore,
                            riskLevel    = smoothedLevel,
                            networkScore = result.ruleScore,
                            backgroundScore = 0,
                            serviceScore = 0,
                            screenOffScore = 0,
                            mlScore      = result.mlScore,
                            anomalyScore = result.anomalyScore,
                            lastUpdated  = System.currentTimeMillis(),
                            isTrusted    = existing?.isTrusted ?: false,
                            trustedUntil = existing?.trustedUntil ?: 0L,
                            reasons      = result.reasons
                                               .joinToString("|")
                        )
                    )

                    scannedCount++

                } catch (e: Exception) {
                    Log.e("ScanWorker",
                        "Failed scanning ${appInfo.packageName}: " +
                        "${e.message}")
                }
            }

            Log.d("ScanWorker",
                "Scan complete: $scannedCount apps, " +
                "$highRiskCount high risk")

            // Send notification if high risk found
            if (highRiskCount > 0) {
                sendHighRiskNotification(
                    highRiskCount, applicationContext
                )
            }

            Result.success(
                workDataOf(
                    "scanned_count"   to scannedCount,
                    "high_risk_count" to highRiskCount
                )
            )
        } catch (e: Exception) {
            Log.e("ScanWorker", "Scan failed: ${e.message}")
            Result.retry()
        }
    }

    private fun sendHighRiskNotification(
        count: Int, 
        context: Context
    ) {
        val nm = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val notification = NotificationCompat
            .Builder(context, SentinelApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("⚠️ SentinelAI Alert")
            .setContentText(
                "$count app${if (count > 1) "s" else ""} " +
                "showing suspicious activity"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(SentinelApp.NOTIF_ID_ALERT, notification)
    }
}
