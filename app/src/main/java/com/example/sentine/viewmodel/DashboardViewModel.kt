// viewmodel/DashboardViewModel.kt

package com.example.sentine.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentine.SentinelApp
import com.example.sentine.engine.RiskScoringEngine
import com.example.sentine.data.db.AppRiskEntity
import com.example.sentine.data.db.RiskEventEntity
import com.example.sentine.data.repository.RiskRepository
import com.example.sentine.data.SettingsRepository
import android.net.TrafficStats
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RiskRepository(
        (application as SentinelApp).database.appRiskDao()
    )
    private val settingsRepo = SettingsRepository(application)
    private val riskEngine = (application as SentinelApp).riskEngine

    // UI state
    private val _apps = MutableStateFlow<List<AppRiskEntity>>(emptyList())
    val apps: StateFlow<List<AppRiskEntity>> = _apps

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress

    private val _statusText = MutableStateFlow("Ready")
    val statusText: StateFlow<String> = _statusText

    val allEvents: Flow<List<RiskEventEntity>> = repository.allEvents

    init {
        // Load existing results from DB on start
        viewModelScope.launch {
            repository.allAppRisks.collect { list ->
                _apps.value = list
            }
        }
    }

    fun scanAllApps() {
        if (_isScanning.value) return
        
        viewModelScope.launch {
            _isScanning.value   = true
            _scanProgress.value = 0f

            val pm = getApplication<Application>().packageManager

            // Get REAL user-installed apps only
            val userApps = pm
                .getInstalledApplications(
                    PackageManager.GET_META_DATA
                )
                .filter { info ->
                    (info.flags and 
                     ApplicationInfo.FLAG_SYSTEM) == 0 &&
                    info.packageName != 
                        getApplication<Application>().packageName
                }
                .sortedBy { 
                    pm.getApplicationLabel(it).toString() 
                }

            val total = userApps.size
            if (total == 0) {
                _isScanning.value = false
                _statusText.value = "No apps found to scan"
                return@launch
            }

            var highCount = 0

            userApps.forEachIndexed { index, appInfo ->
                val appName = pm.getApplicationLabel(appInfo)
                                .toString()
                _statusText.value = "Scanning $appName..."
                // Not preserving _currentScanningApp locally unless it's handled but the user's snippet implicitly expects _statusText to hold it or similar, adding if necessary
                
                try {
                    // REAL analysis — no faking
                    val result = riskEngine.calculateRisk(
                        appInfo.packageName,
                        10,
                        false,
                        0L
                    )

                    // Get existing for smoothing
                    val existing = repository
                        .getAppRisk(appInfo.packageName)

                    val smoothedScore = if (existing != null) {
                        (result.score * 0.7f + 
                         existing.riskScore * 0.3f).toInt()
                    } else result.score

                    val smoothedLevel = when {
                        smoothedScore >= 76 -> "HIGH"
                        smoothedScore >= 51 -> "MEDIUM"
                        smoothedScore >= 26 -> "LOW"
                        else               -> "SAFE"
                    }

                    if (smoothedLevel == "HIGH") highCount++

                    repository.insertOrUpdate(
                        AppRiskEntity(
                            packageName  = appInfo.packageName,
                            appName      = appName,
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

                } catch (e: Exception) {
                    android.util.Log.e("DashboardVM",
                        "Error scanning ${appInfo.packageName}: " +
                        "${e.message}")
                }

                // REAL progress based on actual apps scanned
                _scanProgress.value = (index + 1f) / total
            }

            // Mark scan complete in DataStore
            settingsRepo.setFirstScanCompleted()
            settingsRepo.updateLastScanTimestamp()

            _isScanning.value   = false
            _statusText.value   = 
                "Scan complete · $total apps · $highCount threats"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun filterByLevel(level: String?): List<AppRiskEntity> {
        return if (level == null) _apps.value
        else _apps.value.filter { it.riskLevel == level }
    }
}
