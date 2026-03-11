package com.example.sentine.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sentine.SentinelApp
import com.example.sentine.data.db.AppDatabase
import com.example.sentine.data.db.AppRiskEntity
import com.example.sentine.data.db.RiskEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SentinelWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val riskEngine = (appContext as SentinelApp).riskEngine
            val database = AppDatabase.getDatabase(appContext)
            val pm = appContext.packageManager

            // Check battery status
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                appContext.registerReceiver(null, ifilter)
            }
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = level * 100 / scale.toFloat()
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            // Check network status
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            val isUnmetered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true ||
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            // Determine which apps to scan
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }

            val appsToScan = when {
                batteryPct < 20 && !isCharging -> {
                    // Low battery, not charging: only scan recently updated apps 
                    val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                    allApps.filter { appInfo ->
                        try {
                            val packageInfo = pm.getPackageInfo(appInfo.packageName, 0)
                            packageInfo.lastUpdateTime > oneDayAgo
                        } catch (e: Exception) { false }
                    }
                }
                isCharging && isUnmetered -> {
                    // Optimal conditions: deep scan all apps
                    allApps
                }
                else -> {
                    // Normal conditions: scan all apps
                    allApps
                }
            }

            // Perform scan
            for (appInfo in appsToScan) {
                val existing = database.appRiskDao().getAppRisk(appInfo.packageName)
                val isTrusted = existing?.isTrusted ?: false
                val trustedUntil = existing?.trustedUntil ?: 0L

                val result = riskEngine.calculateRisk(
                    packageName = appInfo.packageName,
                    gracePeriodMinutes = 10,
                    isTrusted = isTrusted,
                    trustedUntil = trustedUntil
                )

                val appRisk = AppRiskEntity(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    riskScore = result.score,
                    riskLevel = result.level,
                    networkScore = result.networkScore,
                    backgroundScore = result.backgroundScore,
                    serviceScore = result.serviceScore,
                    screenOffScore = result.screenOffScore,
                    mlScore = result.mlScore,
                    anomalyScore = result.anomalyScore,
                    lastUpdated = System.currentTimeMillis(),
                    isTrusted = isTrusted,
                    trustedUntil = trustedUntil,
                    isRecentlyUsed = result.isRecentlyUsed,
                    skipReason = result.skipReason,
                    reasons = result.events.joinToString("|")
                )

                database.appRiskDao().insertAppRisk(appRisk)
                
                // Save history
                database.appRiskDao().insertEvent(
                    RiskEventEntity(
                        packageName = appInfo.packageName,
                        eventType = "DAILY_SCORE",
                        eventDetail = result.score.toString(),
                        timestamp = System.currentTimeMillis()
                    )
                )

                if (!result.isRecentlyUsed && result.level == "HIGH") {
                    result.events.forEach { detail ->
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

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
