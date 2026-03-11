package com.example.sentine

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.sentine.data.TrafficStatsCache
import com.example.sentine.data.db.AppDatabase
import com.example.sentine.engine.RiskScoringEngine

class SentinelApp : Application() {

    lateinit var riskEngine: RiskScoringEngine
        private set
    lateinit var database: AppDatabase
        private set
        
    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannels()
        database = AppDatabase.getDatabase(this)
        
        // Initialize ML engine in background when app starts
        riskEngine = RiskScoringEngine(this)
        Thread {
            try {
                riskEngine.initialize()

                // Capture baseline traffic readings BEFORE any scoring happens
                val pm = packageManager
                pm.getInstalledApplications(0)
                    .filter { app ->
                        (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
                    }
                    .forEach { app ->
                        TrafficStatsCache.captureBaseline(app.uid)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Security Alerts"
            val descriptionText = "Notifications for high-risk applications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ALERTS, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ALERTS = "security_alerts"
        const val NOTIF_ID_ALERT = 1001
    }
}
