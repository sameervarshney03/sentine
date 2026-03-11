package com.example.sentine.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.sentine.data.SettingsRepository
import com.example.sentine.data.AppSettings
import com.example.sentine.service.ScanWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = repo.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setMonitoringEnabled(enabled)
            if (enabled) {
                startMonitoringService()
            } else {
                stopMonitoringService()
            }
        }
    }

    private fun startMonitoringService() {
        val context = getApplication<Application>()
        val freq    = settings.value.scanFrequencyMins.toLong()

        WorkManager.getInstance(context).cancelUniqueWork("sentinel_scan")

        val workRequest = PeriodicWorkRequestBuilder<ScanWorker>(
            freq, TimeUnit.MINUTES
        )
        .setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(settings.value.batterySaver)
                .build()
        )
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sentinel_scan",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun stopMonitoringService() {
        val context = getApplication<Application>()
        WorkManager.getInstance(context).cancelUniqueWork("sentinel_scan")
    }

    fun setScanFrequency(minutes: Int) {
        viewModelScope.launch {
            repo.setScanFrequency(minutes)
            if (settings.value.monitoringEnabled) {
                startMonitoringService()
            }
        }
    }

    fun setBatterySaver(enabled: Boolean) {
        viewModelScope.launch {
            repo.setBatterySaver(enabled)
            if (settings.value.monitoringEnabled) {
                startMonitoringService()
            }
        }
    }

    fun setHighRiskAlerts(enabled: Boolean) {
        viewModelScope.launch {
            repo.setHighRiskAlerts(enabled)
        }
    }

    fun setMediumRiskAlerts(enabled: Boolean) {
        viewModelScope.launch {
            repo.setMediumRiskAlerts(enabled)
        }
    }

    fun setQuietHours(enabled: Boolean, start: Int, end: Int) {
        viewModelScope.launch {
            repo.setQuietHours(enabled, start, end)
        }
    }

    fun hasUsageAccessPermission(): Boolean {
        val context = getApplication<Application>()
        val appOps  = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun clearAllData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            repo.clearAllData(context)
            (context as? com.example.sentine.SentinelApp)?.database?.appRiskDao()?.deleteAll()
            WorkManager.getInstance(context).cancelAllWork()
        }
    }

    fun frequencyLabel(minutes: Int): String = when (minutes) {
        2    -> "Every 2 min (charging)"
        5    -> "Every 5 minutes"
        15   -> "Every 15 minutes"
        30   -> "Every 30 minutes"
        9999 -> "Manual only"
        else -> "Every $minutes minutes"
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            repo.setOnboardingCompleted()
        }
    }

    fun getMLStatus(): String {
        return if (
            (getApplication<Application>() as com.example.sentine.SentinelApp)
                .riskEngine.isMLReady
        ) {
            "✅ TFLite model loaded and running"
        } else {
            "⚠️ ML model not loaded — behavioral scoring only"
        }
    }
}
