package com.example.sentine.engine

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.TrafficStats
import android.os.PowerManager
import com.example.sentine.utils.AppUtils

class RiskScoringEngine(private val context: Context) {

    data class RiskResult(
        val score: Int,
        val level: String,
        val networkScore: Int,
        val backgroundScore: Int,
        val serviceScore: Int,
        val screenOffScore: Int,
        val events: List<String>,
        val isRecentlyUsed: Boolean = false,
        val skipReason: String? = null
    )

    fun calculateRisk(
        packageName: String, 
        gracePeriodMinutes: Int = 10,
        isTrusted: Boolean = false
    ): RiskResult {
        val uid = AppUtils.getUidForPackage(context, packageName)
        if (uid == -1) return RiskResult(0, "SAFE", 0, 0, 0, 0, emptyList())

        // Signal 1: Network Upload (35%) - ALWAYS active
        val events = mutableListOf<String>()
        val networkScore = calculateNetworkScore(uid, events)

        // Requirement 1 & 2: User Context Awareness (Grace Period)
        if (isRecentlyUsed(packageName, gracePeriodMinutes)) {
            return RiskResult(
                score = 0,
                level = "SAFE",
                networkScore = networkScore,
                backgroundScore = 0,
                serviceScore = 0,
                screenOffScore = 0,
                events = emptyList(),
                isRecentlyUsed = true,
                skipReason = "App recently used by you — not flagged"
            )
        }

        // Requirement 5: Trusted apps only flag network spikes
        if (isTrusted) {
            val totalScore = (networkScore * 0.35).toInt()
            val level = if (totalScore > 25) "MEDIUM" else "SAFE"
            return RiskResult(
                score = totalScore,
                level = level,
                networkScore = networkScore,
                backgroundScore = 0,
                serviceScore = 0,
                screenOffScore = 0,
                events = events
            )
        }

        // Signal 2: Background Usage Pattern (30%)
        val backgroundScore = calculateBackgroundScore(packageName, events)

        // Signal 3: Active Foreground Service (20%)
        val serviceScore = calculateServiceScore(packageName, events)

        // Signal 4: Screen-Off Activity (15%)
        val screenOffScore = calculateScreenOffScore(uid, packageName, events)

        val totalScore = (networkScore * 0.35 + backgroundScore * 0.30 + serviceScore * 0.20 + screenOffScore * 0.15).toInt()
        
        val level = when {
            totalScore > 75 -> "HIGH"
            totalScore > 50 -> "MEDIUM"
            totalScore > 25 -> "LOW"
            else -> "SAFE"
        }

        return RiskResult(totalScore, level, networkScore, backgroundScore, serviceScore, screenOffScore, events)
    }

    private fun isRecentlyUsed(packageName: String, gracePeriodMinutes: Int): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - gracePeriodMinutes * 60 * 1000
        
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val appStats = stats.find { it.packageName == packageName }
        
        return appStats != null && appStats.lastTimeUsed > startTime
    }

    private fun calculateNetworkScore(uid: Int, events: MutableList<String>): Int {
        val bytesSent = TrafficStats.getUidTxBytes(uid)
        return if (bytesSent > 1024 * 1024) {
            events.add("Significant network upload detected")
            80
        } else if (bytesSent > 100 * 1024) {
            40
        } else 0
    }

    private fun calculateBackgroundScore(packageName: String, events: MutableList<String>): Int {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 30 * 60 * 1000
        
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val appStats = stats.find { it.packageName == packageName }
        
        return if (appStats != null && appStats.totalTimeInForeground < 1000 && appStats.lastTimeUsed > startTime) {
            events.add("Background activity detected without user interaction")
            70
        } else 0
    }

    private fun calculateServiceScore(packageName: String, events: MutableList<String>): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        val runningServices = am.getRunningServices(Int.MAX_VALUE)
        val isRunning = runningServices.any { it.service.packageName == packageName && it.foreground }
        
        return if (isRunning) {
            events.add("Active foreground service detected")
            50
        } else 0
    }

    private fun calculateScreenOffScore(uid: Int, packageName: String, events: MutableList<String>): Int {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isInteractive) {
            val bytesSent = TrafficStats.getUidTxBytes(uid)
            if (bytesSent > 50 * 1024) {
                events.add("Network activity while screen is OFF")
                return 90
            }
        }
        return 0
    }
}
