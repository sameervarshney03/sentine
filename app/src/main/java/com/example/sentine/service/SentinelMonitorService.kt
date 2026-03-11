package com.example.sentine.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.sentine.MainActivity
import com.example.sentine.R
import com.example.sentine.data.db.AppDatabase
import com.example.sentine.data.db.AppRiskEntity
import com.example.sentine.data.db.RiskEventEntity
import com.example.sentine.engine.RiskScoringEngine
import com.example.sentine.utils.AppUtils
import kotlinx.coroutines.*

class SentinelMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var riskEngine: RiskScoringEngine
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        riskEngine = RiskScoringEngine(this)
        database = AppDatabase.getDatabase(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isManual = intent?.getBooleanExtra("EXTRA_MANUAL_SCAN", false) ?: false
        
        val notification = createNotification("SentinelAI is active", "Monitoring background signals...")
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            if (isManual) {
                performScan()
            } else {
                while (isActive) {
                    performScan()
                    delay(5 * 60 * 1000) 
                }
            }
        }

        return START_STICKY
    }

    private suspend fun performScan() {
        val sharedPref = getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
        val gracePeriod = sharedPref.getInt("grace_period", 10)
        
        val apps = AppUtils.getInstalledApps(this)
        var highRiskCount = 0
        val highRiskApps = mutableListOf<AppRiskEntity>()

        apps.forEach { appInfo ->
            val existing = database.appRiskDao().getAppRisk(appInfo.packageName)
            val isTrusted = existing?.isTrusted ?: false
            
            val result = riskEngine.calculateRisk(
                packageName = appInfo.packageName,
                gracePeriodMinutes = gracePeriod,
                isTrusted = isTrusted
            )
            
            val appRisk = AppRiskEntity(
                packageName = appInfo.packageName,
                appName = AppUtils.getAppName(this, appInfo.packageName),
                riskScore = result.score,
                riskLevel = result.level,
                networkScore = result.networkScore,
                backgroundScore = result.backgroundScore,
                serviceScore = result.serviceScore,
                screenOffScore = result.screenOffScore,
                lastUpdated = System.currentTimeMillis(),
                isTrusted = isTrusted,
                isRecentlyUsed = result.isRecentlyUsed,
                skipReason = result.skipReason
            )
            
            database.appRiskDao().insertAppRisk(appRisk)
            
            // Feature 2: Save daily score history
            database.appRiskDao().insertEvent(
                RiskEventEntity(
                    packageName = appInfo.packageName,
                    eventType = "DAILY_SCORE",
                    eventDetail = result.score.toString(),
                    timestamp = System.currentTimeMillis()
                )
            )
            
            if (!result.isRecentlyUsed && result.level == "HIGH") {
                highRiskCount++
                highRiskApps.add(appRisk)
                
                result.reasons.forEach { detail ->
                    database.appRiskDao().insertEvent(
                        RiskEventEntity(
                            packageName = appInfo.packageName,
                            eventType = "RISK_SIGNAL",
                            eventDetail = detail,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        updateSummaryNotification(highRiskCount)

        if (highRiskApps.isNotEmpty()) {
            serviceScope.launch {
                delay(30000)
                showBatchedNotification(highRiskApps)
            }
        }
    }

    private fun showBatchedNotification(apps: List<AppRiskEntity>) {
        if (apps.isEmpty()) return

        val reviewIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.example.sentine.ACTION_REVIEW_HIGH_RISK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val reviewPendingIntent = PendingIntent.getActivity(this, 100, reviewIntent, PendingIntent.FLAG_IMMUTABLE)

        val dismissIntent = Intent(this, com.example.sentine.receiver.NotificationActionReceiver::class.java).apply {
            action = "com.example.sentine.DISMISS_ALL_ALERTS"
            putExtra("NOTIFICATION_ID", BATCHED_NOTIFICATION_ID)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(this, 101, dismissIntent, PendingIntent.FLAG_IMMUTABLE)

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("${apps.size} apps flagged")
            .setSummaryText("High Risk Alerts")
        
        apps.take(5).forEach { app ->
            inboxStyle.addLine("• ${app.appName} showing suspicious behavior")
        }
        if (apps.size > 5) {
            inboxStyle.addLine("... and ${apps.size - 5} more")
        }

        val appNamesText = apps.take(2).joinToString(", ") { it.appName }
        val moreText = if (apps.size > 2) " and ${apps.size - 2} more " else " "
        val contentText = "${apps.size} apps flagged — $appNamesText${moreText}are showing suspicious behavior"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ SentinelAI Alert")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(reviewPendingIntent)
            .addAction(0, "Review", reviewPendingIntent)
            .addAction(0, "Dismiss All", dismissPendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(BATCHED_NOTIFICATION_ID, notification)
    }

    private fun showHighRiskNotification(app: AppRiskEntity, signals: List<String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("packageName", app.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, app.packageName.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)

        val bigText = StringBuilder("The following suspicious signals were detected:\n")
        signals.forEach { bigText.append("• $it\n") }

        val icon = AppUtils.getAppIcon(this, app.packageName)
        val largeIcon = if (icon != null) drawableToBitmap(icon) else null

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ SentinelAI Alert")
            .setContentText("${app.appName} showing suspicious background activity")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(largeIcon)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setGroup(GROUP_ALERTS)
            .setContentIntent(pendingIntent)
            .addAction(0, "View Details", pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(app.packageName.hashCode(), notification)
    }

    private fun updateSummaryNotification(highCount: Int) {
        val content = if (highCount > 0) {
            "Action Required: $highCount high-risk apps found!"
        } else {
            "SentinelAI is protecting your device — All clear"
        }
        
        val notification = createNotification("SentinelAI Monitor", content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SentinelAI Monitor",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setGroup(GROUP_ALERTS)
            .setGroupSummary(false)
            .build()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = Bitmap.createBitmap(
            if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1,
            if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "sentinel_channel"
        private const val NOTIFICATION_ID = 1
        private const val BATCHED_NOTIFICATION_ID = 2
        private const val GROUP_ALERTS = "com.example.sentine.ALERTS"
    }
}
